package ton.sdk.client.modules

import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import org.slf4j.LoggerFactory
import ton.sdk.client.binding.ClientConfig
import ton.sdk.client.jni.{Binding, Handler}
import ton.sdk.client.modules.Api._
import ton.sdk.client.modules.Client._
import ton.sdk.client.modules.Context.{Effect, jsonPrinter, logger}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
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

  def request[P, R, E[_]](params: P)(implicit call: PlainSdkCall[P, R], effect: Effect[E]): E[R] = { // Effect[E[R]]
    implicit val context: Context = this
    val fnName                    = call.functionName
    val jsonIn                    = call.toJson(params)
    effect.fromJson(effect.request(fnName, jsonPrinter.print(jsonIn)))
  }

}

object Context {
  private val jsonPrinter = Printer.spaces2.copy(dropNullValues = true)

  private val errUndefinedBehaviour =
    "Got unfinished error response, the expected behaviour is not clear. Current implementation will continue to consume data but the result will be the first error"
  private val logger = LoggerFactory.getLogger(getClass)

  def create(config: ClientConfig): Try[Context] = this.synchronized {
    val json = Binding.tcCreateContext(jsonPrinter.print(config.asJson))
    SdkResultOrError.fromJson[Long](json).map(Context.apply)
  }

  def local[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T]   = effect.managed(ClientConfig.local)(block)
  def mainNet[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T] = effect.managed(ClientConfig.mainNet)(block)
  def devNet[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T]  = effect.managed(ClientConfig.devNet)(block)
  def testNet[T, E[_]](block: Context => E[T])(implicit effect: Effect[E]): E[T] = effect.managed(ClientConfig.testNet)(block)

//  def requestSync(functionName: String, functionParams: String)(implicit ctx: Context): Try[String] =
//    ctx.request(functionName, functionParams)
//  def requestAsync(functionName: String, functionParams: String)(implicit ctx: Context): Future[String] =
//    ctx.requestAsync(functionName, functionParams)

  def call[P, R, E[_]](params: P)(implicit call: PlainSdkCall[P, R], ctx: Context, eff: Effect[E]): E[R] =
    ctx.request(params)

  // TODO
//  def call[P, R, S, E[_]](params: P)(implicit call: StreamingSdkCall[P, R, S], ctx: Context, eff: Effect[E]): E[R] = {
//    ctx.request(params)
//  }

  trait Effect[T[_]] {
    def request(functionName: String, functionParams: String)(implicit c: Context): T[String]
    def fromTry[R](t: Try[R]): T[R]
    def flatMap[P, R](in: T[P])(f: P => T[R]): T[R]
    def map[P, R](in: T[P])(f: P => R): T[R]
    def recover[R, U >: R](in: T[R])(pf: PartialFunction[Throwable, U]): T[U]
    def fromJson[R](str: T[String])(implicit call: PlainSdkCall[_, R]): T[R] = flatMap(str)(s => fromTry(call.fromJson(s)))
    def managed[R](config: ClientConfig)(block: Context => T[R]): T[R]
    def unsafeGet[R](a: T[R]): R
    def init[R](a: R): T[R]
  }

  val tryEffect: Effect[Try] = new Effect[Try] {
    override def request(functionName: String, functionParams: String)(implicit c: Context): Try[String] = {
      if (!c.isOpen.get()) {
        Failure(new IllegalStateException(s"Request(sync) is called on closed context ${c.id}: $functionName, $functionParams"))
      } else {
        Try(Binding.tcRequestSync(c.id, functionName, functionParams))
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
    override def request(functionName: String, functionParams: String)(implicit c: Context): Future[String] = {
      val p   = Promise[String]()
      val buf = new StringBuilder
      val handler: Handler = (requestId: Long, paramsJson: String, responseType: Long, finished: Boolean) => {
        ResponseType(responseType) match {
          case ResponseTypeNop | ResponseTypeReserved(_) =>
            if (finished) p.success(buf.result())
          case ResponseTypeResult =>
            buf.append(paramsJson)
            if (finished) p.success(buf.result())
          case ResponseTypeError =>
            if (!finished) logger.warn(errUndefinedBehaviour)
            p.failure(SdkClientError(c, requestId, paramsJson).fold(BindingError, identity))
          case ResponseTypeStream(code) =>
            // TODO As for now, the same as Result but probably should be something different
            buf.append(paramsJson)
            if (finished) p.success(buf.result())
        }
      }
      if (!c.isOpen.get()) {
        p.failure(new IllegalStateException(s"Request(async) is called on closed context ${c.id}: $functionName, $functionParams"))
      } else {
        Binding.request(c.id, functionName, functionParams, handler)
      }
      p.future
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

    override def unsafeGet[R](a: Future[R]): R = Await.result(a, 10.seconds)
    override def init[R](a: R): Future[R]      = Future(a)
  }

  def fromTry[R](t: Try[R]): Future[R] = t match {
    case Success(r)  => Future.successful(r)
    case Failure(ex) => Future.failed(ex)
  }
}
