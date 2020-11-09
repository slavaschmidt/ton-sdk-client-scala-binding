package ton.sdk.client.binding

import io.circe._
import io.circe.syntax._

import scala.concurrent.{Future, Promise}
import scala.util._

object Api {

  sealed trait ResponseType {
    def code: Long
  }
  case object ResponseTypeResult                           extends ResponseType { override def code = 0L }
  case object ResponseTypeError                            extends ResponseType { override def code = 1L }
  case object ResponseTypeNop                              extends ResponseType { override def code = 2L }
  case class ResponseTypeReserved(override val code: Long) extends ResponseType
  case class ResponseTypeStream(override val code: Long)   extends ResponseType

  object ResponseType {
    def apply(code: Long): ResponseType = code match {
      case 0                     => ResponseTypeResult
      case 1                     => ResponseTypeError
      case 2                     => ResponseTypeNop
      case x if x > 2 && x < 100 => ResponseTypeReserved(x)
      case x                     => ResponseTypeStream(x)
    }
  }

  /**
    * @param code
    * -1 -> JsonApiParsingError
    *
    * 1 ->  NotImplemented
    * 2 ->  InvalidHex
    * 3 ->  InvalidBase64
    * 4 ->  InvalidAddress
    * 5 ->  CallbackParamsCantBeConvertedToJson
    * 6 ->  WebsocketConnectError
    * 7 ->  WebsocketReceiveError
    * 8 ->  WebsocketSendError
    * 9 ->  HttpClientCreateError
    * 10 ->  HttpRequestCreateError
    * 11 ->  HttpRequestSendError
    * 12 ->  HttpRequestParseError
    * 13 ->  CallbackNotRegistered
    * 14 ->  NetModuleNotInit
    * 15 ->  InvalidConfig
    * 16 ->  CannotCreateRuntime
    * 17 ->  InvalidContextHandle
    * 18 ->  CannotSerializeResult
    * 19 ->  CannotSerializeError
    * 20 ->  CannotConvertJsValueToJson
    * 21 ->  CannotReceiveSpawnedResult
    * 22 ->  SetTimerError
    * 23 ->  InvalidParams
    * 24 ->  ContractsAddressConversionFailed
    * 25 ->  UnknownFunction
    *
    * @param message
    * @param data
    */
  class SdkClientError(val code: Long, val message: String, val data: Json) extends Exception(message)
  class ContextSdkClientError(override val code: Long, override val message: String, context: Context, requestId: Long, override val data: Json)
      extends SdkClientError(code, s"[$code] $message. [$context:$requestId]", data)

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

  abstract class AbstractSdkCall[P: Encoder, R: Decoder] {
    def function: String
    def toJson(parameters: P) = parameters.asJson
    implicit val decoder      = implicitly[Decoder[R]]
  }

  abstract class SdkCall[P: Encoder, R: Decoder] extends AbstractSdkCall[P, R]
  abstract class StreamingSdkCall[P: Encoder, R: Decoder, S: Decoder] extends AbstractSdkCall[P, R] {
    implicit val decoders = (implicitly[Decoder[R]], implicitly[Decoder[S]])
  }

  class AsyncCallResult[R, S](val result: Future[R]) {
    val done     = Promise[Unit]()
    val messages = new QueueBackedIterator[S](done)
    val errors   = new QueueBackedIterator[SdkClientError](done)
  }

}