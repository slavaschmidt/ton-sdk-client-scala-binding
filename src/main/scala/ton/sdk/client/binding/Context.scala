package ton.sdk.client.binding

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

import io.circe.generic.auto._
import io.circe.syntax._
import io.circe._
import org.slf4j.LoggerFactory
import ton.sdk.client.binding.Api._
import ton.sdk.client.binding.Context._
import ton.sdk.client.jni.{Binding, Handler}

import scala.concurrent._
import scala.concurrent.duration.DurationInt
import scala.language.higherKinds
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
    implicit val context: Context    = this
    implicit val decoder: Decoder[R] = call.decoder
    val fnName                       = call.function
    val jsonIn                       = call.toJson(params)
    effect.request(fnName, jsonPrinter.print(jsonIn))
  }

  def requestS[P, R, S, E[_]](params: P)(implicit call: StreamingSdkCall[P, R, S], effect: Effect[E]): E[StreamingCallResult[R, S]] = {
    implicit val context: Context                   = this
    implicit val decoders: (Decoder[R], Decoder[S]) = call.decoders
    val fnName: String                              = call.function
    val jsonIn: Json                                = call.toJson(params)
    val result                                      = effect.requestStreaming(fnName, jsonPrinter.print(jsonIn))
    result
  }
}

object Context {
  type StreamingCallResult[R, S] = (R, BlockingIterator[S], BlockingIterator[SdkClientError])

  private val jsonPrinter = Printer.noSpaces.copy(dropNullValues = true)

  private val errUndefinedBehaviour =
    "Got unfinished error response, the expected behaviour is not clear. Current implementation will continue to consume data but the result will be the first error"
  private val logger = LoggerFactory.getLogger(getClass)

  def create(config: ClientConfig): Try[Context] = this.synchronized {
    val json = Binding.tcCreateContext(jsonPrinter.print(config.asJson))
    SdkResultOrError.fromJsonWrapped[Long](json).map(Context.apply)
  }

  def local[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T]   = effect.managed(ClientConfig.LOCAL)(block)
  def mainNet[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T] = effect.managed(ClientConfig.MAIN_NET)(block)
  def devNet[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T]  = effect.managed(ClientConfig.DEV_NET)(block)
  def testNet[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T] = effect.managed(ClientConfig.TEST_NET)(block)

  def call[P, R, E[_]](params: P)(implicit call: SdkCall[P, R], ctx: Context, eff: Effect[E]): E[R] =
    ctx.request(params)

  def callS[P, R, S, E[_]](params: P)(implicit call: StreamingSdkCall[P, R, S], ctx: Context, eff: Effect[E]): E[StreamingCallResult[R, S]] =
    ctx.requestS(params)

  trait Effect[T[_]] {
    def request[R](functionName: String, functionParams: String)(implicit c: Context, decoder: io.circe.Decoder[R]): T[R]
    def requestStreaming[R, S](functionName: String, functionParams: String)(
      implicit c: Context,
      decoders: (io.circe.Decoder[R], io.circe.Decoder[S])
    ): T[StreamingCallResult[R, S]]
    def fromTry[R](t: Try[R]): T[R]
    def flatMap[P, R](in: T[P])(f: P => T[R]): T[R]
    def map[P, R](in: T[P])(f: P => R): T[R]
    def recover[R, U >: R](in: T[R])(pf: PartialFunction[Throwable, U]): T[U]
    def managed[R](config: ClientConfig)(block: Context => T[R]): T[R]
    def unsafeGet[R](a: T[R]): R
    def init[R](a: R): T[R]
  }

  val tryEffect: Effect[Try] = new Effect[Try] {
    override def requestStreaming[R, S](functionName: String, functionParams: String)(
      implicit c: Context,
      decoders: (io.circe.Decoder[R], io.circe.Decoder[S])
    ): Try[StreamingCallResult[R, S]] =
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
    override def requestStreaming[R, S](
      functionName: String,
      functionParams: String
    )(implicit c: Context, r: (Decoder[R], Decoder[S])): Future[StreamingCallResult[R, S]] =
      requestStreamingFuture(functionName, functionParams)

    override def request[R](functionName: String, functionParams: String)(implicit c: Context, d: Decoder[R]): Future[R] =
      requestFuture(functionName, functionParams)

    private def requestStreamingFuture[R, S](
      functionName: String,
      functionParams: String
    )(implicit c: Context, r: (Decoder[R], Decoder[S])): Future[StreamingCallResult[R, S]] = {
      val p      = Promise[R]()
      val result = new AsyncCallResult[R, S](p.future)
      val handler: Handler = (requestId: Long, paramsJson: String, responseType: Long, finished: Boolean) => {
        logger.trace(s"Streaming $requestId: $responseType ($finished) - $paramsJson")
        if (finished) result.messages.close(None)
        ResponseType(responseType) match {
          case ResponseTypeNop | ResponseTypeReserved(_) =>
            println(s"STREAMING ($finished): NOP or Reserved $paramsJson")
          case ResponseTypeResult =>
            println(s"STREAMING ($finished): result $paramsJson")
            implicit val decoder = r._1
            successIfFinished(requestId, finished, p, paramsJson)
          case ResponseTypeError =>
            println(s"STREAMING ($finished): error $paramsJson")
            p.failure(SdkClientError(c, requestId, paramsJson).fold(BindingError, identity))
          case ResponseTypeStream(code) =>
            println(s"STREAMING ($finished): stream ($code) $paramsJson")
            implicit val decoder = r._2
            val _ = SdkResultOrError.fromJsonPlain[S](requestId, paramsJson) match {
              case Failure(er: SdkClientError) => result.errors.append(er)
              case Success(s)                  => result.messages.append(s)
              case Failure(er)                 => logger.warn("Unexpected streaming error", er)
            }
        }
      }
      if (!c.isOpen.get()) {
        p.failure(new IllegalStateException(s"Request(async) is called on closed context ${c.id}: $functionName, $functionParams"))
      } else {
        println(s"calling async streaming $functionName - $functionParams")
        Binding.request(c.id, functionName, functionParams, handler)
      }
      result.result.map((_, result.messages, result.errors))
    }

    private def requestFuture[R, S](functionName: String, functionParams: String)(implicit c: Context, r: Decoder[R]): Future[R] = {
      val p   = Promise[R]()
      val buf = StringBuilder.newBuilder
      val handler: Handler = (requestId: Long, paramsJson: String, responseType: Long, finished: Boolean) => {
        println(s"$requestId: $responseType ($finished) - $paramsJson - ${buf.result}")
        if (finished && p.isCompleted) {
          println(s"FUCK, finished & completed already: $requestId: $responseType ($finished) - $paramsJson")
        }
        ResponseType(responseType) match {
          case ResponseTypeNop | ResponseTypeReserved(_) =>
            logger.debug(s"Got NOP or RESERVED, promise state: ${p.isCompleted}, should be finished: $finished: $paramsJson")
            successIfFinished(requestId, finished, p, buf.result())

          case ResponseTypeResult =>
            buf.append(paramsJson)
            successIfFinished(requestId, finished, p, buf.result())

          case ResponseTypeError =>
            if (!finished) logger.warn(errUndefinedBehaviour)
            p.failure(SdkClientError(c, requestId, paramsJson).fold(BindingError, identity))

          case ResponseTypeStream(code) =>
            logger.warn(s"Streaming in non-streaming request: $requestId: $responseType[$code]($finished) - $paramsJson")
            buf.append(paramsJson)
            successIfFinished(requestId, finished, p, buf.result())
        }
      }
      if (!c.isOpen.get()) {
        p.failure(new IllegalStateException(s"Request(async) is called on closed context ${c.id}: $functionName, $functionParams"))
      } else {
        Binding.request(c.id, functionName, functionParams, handler)
      }
      p.future
    }

    private def successIfFinished[R: Decoder](requestId: Long, finished: Boolean, p: Promise[R], buf: String): Unit = {
      val _ =
        if (finished && !p.isCompleted)
          SdkResultOrError.fromJsonPlain(requestId, buf).fold(ex => p.failure(SdkClientError.parsingError(requestId, ex.getMessage, buf.asJson)), p.success(_))
    }

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

    override def unsafeGet[R](a: Future[R]): R = Await.result(a, 60.seconds)
    override def init[R](a: R): Future[R]      = Future(a)

  }

  def fromTry[R](t: Try[R]): Future[R] = t match {
    case Success(r)  => Future.successful(r)
    case Failure(ex) => Future.failed(ex)
  }

}
