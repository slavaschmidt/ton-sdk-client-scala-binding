package ton.sdk.client.modules

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

import io.circe
import io.circe.Decoder.Result
import io.circe.generic.auto._
import io.circe.jawn.decode
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, HCursor, Json, Printer}
import org.slf4j.LoggerFactory
import ton.sdk.client.binding.{ClientConfig, Handle}
import ton.sdk.client.jni.{Binding, Handler, SdkCallback}
import ton.sdk.client.modules.Api._
import ton.sdk.client.modules.Context.{jsonPrinter, logger, Effect}

import scala.concurrent.duration.DurationInt
import scala.concurrent._
import scala.util.{Failure, Success, Try}

/**
  * The context should be explicitly closed after it is not needed any more
  */
final case class Context private (id: Long) extends Closeable {
  val isOpen = new AtomicBoolean(true)
  @throws[Exception]
  override def close(): Unit =
    try {
      if (isOpen.getAndSet(false)) Binding.tcDestroyContext(id)
    } catch {
      case ex: Throwable => logger.warn(s"Failed to close Context($id): ${ex.getMessage}")
    }

  override def finalize(): Unit = if (isOpen.get()) {
    logger.warn(s"Context($id) was not closed as expected, this is a programming error")
  }

  def request[P, R, E[_]](params: P)(implicit call: SdkCall[P, R], effect: Effect[E]): E[R] = {
    implicit val context: Context = this
    val fnName                    = call.function
    val jsonIn                    = call.toJson(params)
    import call.decoder
    effect.request(fnName, jsonPrinter.print(jsonIn))
  }

  def request[P, S, E[_]](params: P, callback: SdkCallback[S])(implicit call: StreamingSdkCall[P, S], effect: Effect[E]): E[Handle] = {
    implicit val context: Context = this
    val fnName                    = call.function
    val jsonIn                    = call.toJson(params)
    import call.decoders
    effect.request(fnName, jsonPrinter.print(jsonIn), callback)
  }
}

object Context {

  private val jsonPrinter = Printer.noSpaces.copy(dropNullValues = true)

  private val errUndefinedBehaviour =
    "Got unfinished error response, the expected behaviour is not clear. Current implementation will continue to consume data but the result will be the first error"
  private val logger = LoggerFactory.getLogger(getClass)

  def create(config: ClientConfig): Try[Context] = this.synchronized {
    val json = Binding.tcCreateContext(jsonPrinter.print(config.asJson))
    SdkResultOrError.fromJsonWrapped[Long](json).map(Context.apply)
  }

  def local[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T]   = effect.managed(ClientConfig.local)(block)
  def mainNet[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T] = effect.managed(ClientConfig.mainNet)(block)
  def devNet[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T]  = effect.managed(ClientConfig.devNet)(block)
  def testNet[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T] = effect.managed(ClientConfig.testNet)(block)

  def call[P, R, E[_]](params: P)(implicit call: SdkCall[P, R], ctx: Context, eff: Effect[E]): E[R] =
    ctx.request(params)

  def call[P, S, E[_]](params: P, callback: SdkCallback[S])(implicit call: StreamingSdkCall[P, S], ctx: Context, eff: Effect[E]): E[Handle] =
    ctx.request(params, callback)

  trait Effect[T[_]] {
    def request[R](functionName: String, functionParams: String)(implicit c: Context, decoder: io.circe.Decoder[R]): T[R]
    def request[R, S](functionName: String, functionParams: String, callback: SdkCallback[S])(implicit c: Context, decoders: (io.circe.Decoder[R], io.circe.Decoder[S])): T[R]
    def fromTry[R](t: Try[R]): T[R]
    def flatMap[P, R](in: T[P])(f: P => T[R]): T[R]
    def map[P, R](in: T[P])(f: P => R): T[R]
    def recover[R, U >: R](in: T[R])(pf: PartialFunction[Throwable, U]): T[U]
    def managed[R](config: ClientConfig)(block: Context => T[R]): T[R]
    def unsafeGet[R](a: T[R]): R
    def init[R](a: R): T[R]
  }

  val tryEffect: Effect[Try] = new Effect[Try] {
    override def request[R, S](functionName: String, functionParams: String, callback: SdkCallback[S])(
      implicit c: Context,
      decoders: (io.circe.Decoder[R], io.circe.Decoder[S])
    ): Try[R] =
      Failure(new SdkClientError(-1, s"Streaming synchronous requests aren't supported (function $functionName)", Json.Null))

    override def request[R](functionName: String, functionParams: String)(implicit c: Context, decoder: io.circe.Decoder[R]): Try[R] = {
      if (!c.isOpen.get()) {
        Failure(new IllegalStateException(s"Request(sync) is called on closed context ${c.id}: $functionName, $functionParams"))
      } else {
        Try {
          val json = Binding.tcRequestSync(c.id, functionName, functionParams)
          SdkResultOrError.fromJsonWrapped(json)
        }.flatten
      }
    }
    override def fromTry[R](t: Try[R]): Try[R] = identity(t)
    override def managed[R](config: ClientConfig)(block: Context => Try[R]): Try[R] = {
      create(config).flatMap { c =>
        val result = Try(block(c)).flatten
        Try(c.close())
        result
      }
    }
    override def flatMap[P, R](in: Try[P])(f: P => Try[R]): Try[R]                         = in.flatMap(f)
    override def map[P, R](in: Try[P])(f: P => R): Try[R]                                  = in.map(f)
    override def unsafeGet[R](a: Try[R]): R                                                = a.get
    override def init[R](a: R): Try[R]                                                     = Try(a)
    override def recover[R, U >: R](in: Try[R])(pf: PartialFunction[Throwable, U]): Try[U] = in.recover(pf)

  }

  def futureEffect(implicit ec: ExecutionContext): Effect[Future] = new Effect[Future] {
    override def request[R, S](functionName: String, functionParams: String, callback: SdkCallback[S])(implicit c: Context, r: (Decoder[R], Decoder[S])): Future[R] =
      requestFuture(functionName, functionParams, Option(callback))

    override def request[R](functionName: String, functionParams: String)(implicit c: Context, d: Decoder[R]): Future[R] = {
      val noneDecoder: Decoder[Any] = new Decoder[Any] {
        override def apply(c: HCursor): Result[Any] = Left(DecodingFailure("Should not be called", Nil))
      }
      implicit val decoders = (d, noneDecoder)
      requestFuture(functionName, functionParams, None)
    }

    // TODO use the callback, boy
    private def requestFuture[R, S](functionName: String, functionParams: String, callback: Option[SdkCallback[S]])(implicit c: Context, r: (Decoder[R], Decoder[S])): Future[R] = {
      val p = Promise[R]()
      val handler: Handler = (requestId: Long, paramsJson: String, responseType: Long, finished: Boolean) => {
        logger.trace(s"$requestId: $responseType ($finished) - $paramsJson")
        println(s"$requestId: $responseType ($finished) - $paramsJson")
        ResponseType(responseType) match {
          case ResponseTypeNop | ResponseTypeReserved(_) =>
            logger.debug(s"Got NOP or RESERVED, promise state: ${p.isCompleted}")

          case ResponseTypeResult =>
            implicit val decoder = r._1
            if (!finished) {
              logger.debug("Not finished, a handle is expected")
              if (decode[Handle](paramsJson).isRight) {
                successIfFinished(finished = true, p, paramsJson)
              } else {
                println("WTF:" + paramsJson)
              }
            } else {
              successIfFinished(finished, p, paramsJson)
            }
          case ResponseTypeError =>
            if (!finished) logger.warn(errUndefinedBehaviour)
            // TODO is this always true? Should callback be taken into account?
            p.failure(SdkClientError(c, requestId, paramsJson).fold(BindingError, identity))

          case ResponseTypeStream(code) =>
            implicit val decoder = r._2
            callback.foreach { cb =>
              decode[S](paramsJson) match {
                case Left(ex) =>
                  decode[SdkClientError](paramsJson) match {
                    case Left(_)  => cb.onFailure(finished, SdkClientError.parsingError(ex.getMessage, paramsJson.asJson))
                    case Right(er) => cb.onFailure(finished, er)
                  }
                case Right(s) => cb.onSuccess(finished, code, s)
              }
            }
        }
      }
      if (!c.isOpen.get()) {
        p.failure(new IllegalStateException(s"Request(async) is called on closed context ${c.id}: $functionName, $functionParams"))
      } else {
        println(s"$functionName - $functionParams")
        Binding.request(c.id, functionName, functionParams, handler)
      }
      p.future
    }

    private def successIfFinished[R: Decoder](finished: Boolean, p: Promise[R], buf: String): Unit =
      if (finished && !p.isCompleted)
        SdkResultOrError.fromJsonPlain(buf).fold(ex => p.failure(SdkClientError.parsingError(ex.getMessage, buf.asJson)), p.success(_))

    override def fromTry[R](t: Try[R]): Future[R] = Context.fromTry(t)

    override def managed[R](config: ClientConfig)(block: Context => Future[R]): Future[R] =
      fromTry(create(config)).flatMap { context =>
        val result = block(context)
        result.onComplete(_ => context.close())
        result
      }
    override def flatMap[P, R](in: Future[P])(f: P => Future[R]): Future[R]                      = in.flatMap(f)
    override def map[P, R](in: Future[P])(f: P => R): Future[R]                                  = in.map(f)
    override def recover[R, U >: R](in: Future[R])(pf: PartialFunction[Throwable, U]): Future[U] = in.recover(pf)

    override def unsafeGet[R](a: Future[R]): R = Await.result(a, 10.seconds)
    override def init[R](a: R): Future[R]      = Future(a)

  }

  def fromTry[R](t: Try[R]): Future[R] = t match {
    case Success(r)  => Future.successful(r)
    case Failure(ex) => Future.failed(ex)
  }

  implicit def fn2callback[S](f: (Boolean, Long, S) => Unit): SdkCallback[S] = new SdkCallback[S] {
    private val logger                                                            = LoggerFactory.getLogger(getClass)
    override def onSuccess(finished: Boolean, responseType: Long, input: S): Unit = f(finished, responseType, input)

    override def onFailure(finished: Boolean, failure: SdkClientError): Unit = {
      val f = if (finished) "finished " else ""
      logger.warn(s"Error in $f streaming response: $failure")
    }
  }

}
