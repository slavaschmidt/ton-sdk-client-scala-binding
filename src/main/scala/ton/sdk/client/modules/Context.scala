package ton.sdk.client.modules

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

import io.circe.generic.auto._
import io.circe.syntax._
import org.slf4j.LoggerFactory
import ton.sdk.client.jni.{Binding, Handler}
import ton.sdk.client.modules.Context.{errUndefinedBehaviour, logger}
import ton.sdk.client.modules.Api._
import ton.sdk.client.modules.Client._

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try, Using}

/**
  * The context should be explicitly closed after it is not needed any more
  */
final case class Context private (id: Long) extends Closeable {
  private val isOpen = new AtomicBoolean(true)

  @throws[Exception]
  override def close(): Unit =
    try {
      if (isOpen.getAndSet(false)) Binding.tcDestroyContext(1)
    } catch {
      case ex: Throwable => logger.warn(s"Failed to close Context($id): ${ex.getMessage}")
    }

  override def finalize(): Unit = if (isOpen.get()) {
    logger.warn(s"Context($id) was not closed as expected, this is a programming error")
  }

  def requestStr(functionName: String, functionParams: String): Try[String] = Try(Binding.tcRequestSync(id, functionName, functionParams))
  def requestAsync(functionName: String, functionParams: String): Future[String] = {
    val p   = Promise[String]()
    val buf = new StringBuilder
    val handler: Handler = (requestId: Long, paramsJson: String, responseType: Long, finished: Boolean) => {
      ResponseType(responseType) match {
        case ResponseTypeNop | ResponseTypeReserved(_) => // ignore
        case ResponseTypeResult                        => buf.append(paramsJson)
        case ResponseTypeError =>
          if (!finished) logger.warn(errUndefinedBehaviour)
          p.failure(SdkClientError(paramsJson).fold(BindingError, identity))
        case ResponseTypeStream(code) =>
          // TODO As for now, the same as Result but probably should be something different
          buf.append(paramsJson)
      }
      if (finished && !p.isCompleted) {
        p.success(buf.result())
      }
    }
    Binding.request(id, functionName, functionParams, handler)
    p.future
  }

  def request[P, R](params: P)(implicit call: SdkCall[P, R], ec: ExecutionContext): Future[R] = {
    val fnName = call.functionName
    val jsonIn = call.toJson(params).noSpaces
    requestAsync(fnName, jsonIn).map(call.fromJson).map(Context.asFuture).flatten
  }

  //
}

object Context {
  def asFuture[R](t: Try[R]): Future[R] = t match {
    case Success(r)  => Future.successful(r)
    case Failure(ex) => Future.failed(ex)
  }

  private val errUndefinedBehaviour =
    "Got unfinished error response, the expected behaviour is not clear. Current implementation will continue to consume data but the result will be the first error"
  private val logger = LoggerFactory.getLogger(getClass)

  type Handler = (Long, String, Long, Boolean) => Unit
  def create(config: ClientConfig): Try[Context] = {
    val json = Binding.tcCreateContext(config.asJson.noSpaces)
    SdkResultOrError.fromJson[Long](json).map(Context.apply)
  }
  def synchronous[T](config: ClientConfig)(block: Context => T): Try[T] = create(config).flatMap(Using(_)(block))
  def async[T](config: ClientConfig)(block: Context => Future[T])(implicit ec: ExecutionContext): Future[T] =
    asFuture(create(config)).flatMap { context =>
      val result = block(context)
      result.onComplete(_ => context.close())
      result
    }

  def local[T](block: Context => Future[T])(implicit ec: ExecutionContext): Future[T]   = Context.async(ClientConfig.local)(block)
  def mainNet[T](block: Context => Future[T])(implicit ec: ExecutionContext): Future[T] = Context.async(ClientConfig.mainNet)(block)
  def devNet[T](block: Context => Future[T])(implicit ec: ExecutionContext): Future[T]  = Context.async(ClientConfig.devNet)(block)
  def testNet[T](block: Context => Future[T])(implicit ec: ExecutionContext): Future[T] = Context.async(ClientConfig.testNet)(block)

  def requestStr(functionName: String, functionParams: String)(implicit ctx: Context): Try[String] =
    ctx.requestStr(functionName, functionParams)
  def requestAsync(functionName: String, functionParams: String)(implicit ctx: Context): Future[String] =
    ctx.requestAsync(functionName, functionParams)

  def call[P, R](params: P)(implicit call: SdkCall[P, R], ctx: Context, ec: ExecutionContext): Future[R] =
    ctx.request(params)

}
