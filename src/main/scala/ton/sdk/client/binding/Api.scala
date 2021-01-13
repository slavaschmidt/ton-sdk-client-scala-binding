package ton.sdk.client.binding

import io.circe._
import io.circe.syntax._

import scala.concurrent.Future
import scala.util._

/**
  * The [[Api]] defines binding - level types and constants as scala case classes and objects.
  * Also defines a main abstraction of the SDK client function call.
  * This implementation uses
  */
object Api {

  sealed trait ResponseType {
    def code: Long
  }
  case object ResponseTypeResult     extends ResponseType { override def code = 0L }
  case object ResponseTypeError      extends ResponseType { override def code = 1L }
  case object ResponseTypeNop        extends ResponseType { override def code = 2L }
  case object ResponseTypeAppRequest extends ResponseType { override def code = 3L }
  case object ResponseTypeAppNotify  extends ResponseType { override def code = 4L }

  final case class ResponseTypeReserved(override val code: Long) extends ResponseType
  final case class ResponseTypeStream(override val code: Long)   extends ResponseType

  object ResponseType {
    def apply(code: Long): ResponseType = code match {
      case 0            => ResponseTypeResult
      case 1            => ResponseTypeError
      case 2            => ResponseTypeNop
      case 3            => ResponseTypeAppRequest
      case 4            => ResponseTypeAppNotify
      case x if x < 100 => ResponseTypeReserved(x)
      case x            => ResponseTypeStream(x)
    }
  }

  type DebotCallback = (ResponseType, Json) => Unit

  type DebotHandle = Int


  /**
    * @param code the client error code
    * @param message the error message
    * @param data the underlying data
    */
  class SdkClientError(val code: Long, val message: String, val data: Json)                                                                      extends Exception(message)
  class ContextSdkClientError(override val code: Long, override val message: String, context: Context, requestId: Long, override val data: Json) extends SdkClientError(code, s"[$code] $message. [$context:$requestId]", data)

  case class BindingError(cause: Throwable) extends Exception(cause)

  object SdkClientError {
    import io.circe.generic.auto._
    import io.circe.parser._
    def apply(c: Context, requestId: Long, json: String): Try[ContextSdkClientError] =
      decode[SdkClientError](json).toTry.map(e => new ContextSdkClientError(e.code, e.message, c, requestId, e.data))
    def parsingError(requestId: Long, message: String, data: Json) =
      new SdkClientError(-1 * requestId, s"Could not parse SDK json: [$message]", data)
  }

  sealed trait SdkResultOrError[T]
  final case class SdkResult[T](result: T) extends SdkResultOrError[T]
  final case class SdkError[T](error: SdkClientError) extends SdkResultOrError[T] {
    override def toString: String = error.toString
  }

  /**
    * As there are two ways sync and async client calls return the result we need to parse both differently
    */
  object SdkResultOrError {
    import io.circe.generic.auto._
    import io.circe.parser._

    def fromJsonWrapped[T: Decoder](json: String): Try[T] = {
      val resp: Either[io.circe.Error, SdkResultOrError[T]] = decode[SdkError[T]](json) match {
        case Left(_)          => decode[SdkResult[T]](json)
        case r @ Right(value) => r
      }
      resp match {
        case Left(error)            => Failure(error)
        case Right(SdkError(error)) => Failure(error)
        case Right(SdkResult(t))    => Success(t)
      }
    }
    def fromJsonPlain[T](requestId: Long, json: String)(implicit decoder: io.circe.Decoder[T]): Try[T] = decode[T](json) match {
      case Left(error)            => Failure(SdkClientError.parsingError(requestId, error.getMessage, json.asJson))
      case Right(r: T @unchecked) => Success(r)
    }
  }

  /**
    * This is the most common abstraction of the SDK client call
    *
    * For the call parameter a json encoder must be available
    * For the result parameter a json decoder must be available
    *
    * @tparam P - the type of the call parameter
    * @tparam R - the type of the result
    */
  abstract class AbstractSdkCall[P: Encoder, R: Decoder] {
    def function: String
    def toJson(parameters: P) = parameters.asJson
    implicit val decoder      = implicitly[Decoder[R]]
  }

  /**
    * Representation of the normal (non-streaming) SDK client call.
    * Can be used to call both sync and async methods.
    *
    * For the call parameter a json encoder must be available
    * For the result parameter a json decoder must be available
    *
    * @tparam P - the type of the call parameter
    * @tparam R - the type of the result
    */
  abstract class SdkCall[P: Encoder, R: Decoder] extends AbstractSdkCall[P, R]

  /**
    * Representation of the streaming SDK client call.
    * Only available for the async calls.
    *
    * For the call parameter a json encoder must be available
    * For the result parameter a json decoder must be available
    *
    * @tparam P - the type of the call parameter
    * @tparam R - the type of the result
    */
  abstract class StreamingSdkCall[P: Encoder, R: Decoder, S: Decoder] extends AbstractSdkCall[P, R] {
    implicit val decoders = (implicitly[Decoder[R]], implicitly[Decoder[S]])
  }

  /**
    * Representation of the debot SDK client call.
    * Can be used to call both sync and async methods.
    *
    * For the call parameter a json encoder must be available
    * For the result parameter a json decoder must be available
    *
    * @tparam P - the type of the call parameter
    * @tparam D - the type of the debot callback
    */
  abstract class DebotCall[P: Encoder, R: Decoder] extends SdkCall[P, R]

  /**
    * For the async call result we need a way to make messages and errors available to the caller.
    * This class represents this way.
    *
    * @param result - representation of the result of the call
    * @tparam R - type of the normal result
    * @tparam S - expected type of the messages
    */
  class AsyncCallResult[R, S](val result: Future[R]) {
    // messages pushed by the callback
    val messages = new QueueBackedIterator[S]()
    // errors pushed by the callback
    val errors = new QueueBackedIterator[SdkClientError]()
  }

}
