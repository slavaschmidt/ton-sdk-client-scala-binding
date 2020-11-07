package ton.sdk.client.modules

import io.circe.{Json, ParsingFailure}
import ton.sdk.client.binding.{CallSet, DeploySet, FunctionHeader, Signer}
import ton.sdk.client.modules.Api._

import scala.io.Source

// TODO status WIP
object Abi {

  private val prefix = "abi"

  case class Parameter(
    components: Option[Seq[Parameter]],
    name: String,
    `type`: String
  )
  case class Function(
    name: String,
    inputs: Seq[Parameter],
    outputs: Seq[Parameter]
  )

  final case class Abi(`type`: String, value: Json)
  object Abi {
    import io.circe.syntax._
    import io.circe.parser._
    val handle                                                   = Abi("Handle", 0.asJson)
    def fromJson(abiJson: Json): Abi                             = Abi("Serialized", abiJson)
    def fromString(abiJson: String): Either[ParsingFailure, Abi] = parse(abiJson).map(fromJson)
    def fromFile(path: String): Either[ParsingFailure, Abi]      = fromString(Source.fromFile(path).mkString)
  }

//  final case class AbiHandle(handle: Int) extends Abi
//  final case class AbiContract(
//    `ABI version`: Double,
//    header: List[String],
//    functions: List[Function],
//    data: Option[List[String]], // TODO empty array, need more examples
//    events: List[Function]
//  ) extends Abi

  final case class StateInitParams(abi: Abi, value: Json)

  case class AbiCallSet(function_name: String, header: Option[FunctionHeader] = None, input: Option[Map[String, Json]] = None)

  case class MessageBodyType(body_type: String)
  object MessageBodyType {
    val input          = "Input"
    val output         = "Output"
    val internalOutput = "InternalOutput"
    val event          = "Event"
  }

  sealed trait StateInitSource
  final case class MessageStateInitSource(source: MessageSource)                                                     extends StateInitSource
  final case class StateStateInitSource(code: String, data: String, library: Option[String])                         extends StateInitSource
  final case class TvcStateInitSource(tvc: String, public_key: Option[String], init_params: Option[StateInitParams]) extends StateInitSource

  sealed trait MessageSource
  final case class EncodedMessageSource(message: String, abi: Option[Abi])    extends MessageSource
  final case class EncodingParamsMessageSource(params: Request.EncodeMessage) extends MessageSource

  object Request {
    final case class EncodeMessageBody(abi: Abi, call_set: AbiCallSet, is_internal: Boolean, signer: Signer, processing_try_index: Option[Int])
    final case class AttachSignatureToMessageBody(abi: Abi, public_key: String, message: String, signature: String)
    final case class EncodeMessage(abi: Abi, address: Option[String], deploy_set: Option[DeploySet], call_set: Option[CallSet], signer: Signer, processing_try_index: Option[Int])
    final case class AttachSignature(abi: Abi, public_key: String, message: String, signature: String)
    final case class DecodeMessage(abi: Abi, message: String)
    final case class DecodeMessageBody(abi: Abi, body: String, is_internal: Boolean)
    final case class EncodeAccount(state_init: StateInitSource, balance: Option[BigInt], last_trans_lt: Option[BigInt], last_paid: Option[BigDecimal])

  }
  object Result {
    final case class EncodeMessageBody(body: String, data_to_sign: Option[String])
    final case class AttachSignatureToMessageBody(body: String)
    final case class EncodeMessage(message: String, data_to_sign: Option[String], address: String, message_id: String)
    final case class AttachSignature(message: String, message_id: String)
    final case class EncodeAccount(account: String, id: String)
    final case class DecodedMessageBody(body_type: String, name: String, value: Option[Json], header: Option[FunctionHeader])
  }

  import io.circe.generic.auto._

  implicit val encodeMessageBody = new SdkCall[Request.EncodeMessageBody, Result.EncodeMessageBody]  { override val function: String = s"$prefix.encode_message_body" }
  implicit val decodeMessage     = new SdkCall[Request.DecodeMessage, Result.DecodedMessageBody]     { override val function: String = s"$prefix.decode_message"      }
  implicit val encodeMessage     = new SdkCall[Request.EncodeMessage, Result.EncodeMessage]          { override val function: String = s"$prefix.encode_message"      }
  implicit val attachSignature   = new SdkCall[Request.AttachSignature, Result.AttachSignature]      { override val function: String = s"$prefix.attach_signature"    }
  implicit val decodeMessageBody = new SdkCall[Request.DecodeMessageBody, Result.DecodedMessageBody] { override val function: String = s"$prefix.decode_message_body" }
  implicit val encodeAccount     = new SdkCall[Request.EncodeAccount, Result.EncodeAccount]          { override val function: String = s"$prefix.encode_account"      }
  implicit val attachSToMB = new SdkCall[Request.AttachSignatureToMessageBody, Result.AttachSignatureToMessageBody] {
    override val function: String = s"$prefix.attach_signature_to_message_body"
  }
}
