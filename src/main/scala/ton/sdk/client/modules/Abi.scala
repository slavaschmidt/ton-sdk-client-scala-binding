package ton.sdk.client.modules

import io.circe.{Json, ParsingFailure}
import ton.sdk.client.binding.{CallSet, DeploySet, FunctionHeader, Signer}
import ton.sdk.client.binding.Api._

import scala.io.Source

// TODO status FAILING
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

  final case class AbiJson(`type`: String, value: Json)
  object AbiJson {
    import io.circe.syntax._
    import io.circe.parser._
    val handle                                                       = AbiJson("Handle", 0.asJson)
    def fromJson(abiJson: Json): AbiJson                             = AbiJson("Serialized", abiJson)
    def fromString(abiJson: String): Either[ParsingFailure, AbiJson] = parse(abiJson).map(fromJson)
    def fromFile(path: String): Either[ParsingFailure, AbiJson]      = fromString(Source.fromFile(path).mkString)
    def fromResource(name: String): Either[ParsingFailure, AbiJson]  = fromFile(getClass.getClassLoader.getResource(name).getFile)
  }

  final case class StateInitParams(abi: AbiJson, value: Json)

  case class AbiCallSet(function_name: String, header: Option[FunctionHeader] = None, input: Option[Map[String, Json]] = None)

  case class MessageBodyType(body_type: String)
  object MessageBodyType {
    val input          = "Input"
    val output         = "Output"
    val internalOutput = "InternalOutput"
    val event          = "Event"
  }

  final case class StateInitSource(
    `type`: String,
    code: Option[String],
    data: Option[String],
    library: Option[String],
    tvc: Option[String],
    public_key: Option[String],
    init_params: Option[StateInitParams],
    source: Option[MessageSource]
  )
  object StateInitSource {
    def fromMessage(source: MessageSource) =
      StateInitSource("Message", None, None, None, None, None, None, Option(source))
    def fromStateInit(code: String, data: String, library: Option[String]) =
      StateInitSource("StateInitSource", Option(code), Option(data), library, None, None, None, None)
    def fromTvc(tvc: String, public_key: Option[String], init_params: Option[StateInitParams]) =
      StateInitSource("Tvc", None, None, None, Option(tvc), public_key, init_params, None)
  }

  final case class MessageSource(
    `type`: String,
    message: Option[String],
    abi: Option[AbiJson],
    address: Option[String],
    deploy_set: Option[DeploySet],
    call_set: Option[CallSet],
    signer: Option[Signer],
    processing_try_index: Option[Int]
  )
  object MessageSource {
    def fromEncoded(message: String, abi: Option[AbiJson]) = MessageSource("Encoded", Option(message), abi, None, None, None, None, None)
    def fromEncodingParams(p: Request.EncodeMessage) = {
      MessageSource("EncodingParams", None, Option(p.abi), p.address, p.deploy_set, p.call_set, Option(p.signer), Option(p.processing_try_index))
    }
  }

  object Request {
    final case class EncodeMessageBody(abi: AbiJson, call_set: AbiCallSet, is_internal: Boolean, signer: Signer, processing_try_index: Option[Int])
    final case class AttachSignatureToMessageBody(abi: AbiJson, public_key: String, message: String, signature: String)
    final case class EncodeMessage(abi: AbiJson, address: Option[String], deploy_set: Option[DeploySet], call_set: Option[CallSet], signer: Signer, processing_try_index: Int = 0)
    final case class AttachSignature(abi: AbiJson, public_key: String, message: String, signature: String)
    final case class DecodeMessage(abi: AbiJson, message: String)
    final case class DecodeMessageBody(abi: AbiJson, body: String, is_internal: Boolean)
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
