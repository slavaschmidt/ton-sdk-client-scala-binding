package ton.sdk.client.modules

import io.circe.{Json, ParsingFailure}
import ton.sdk.client.binding.{CallSet, DeploySet, FunctionHeader, Signer}
import ton.sdk.client.binding.Api._

import scala.io.Source

/**
  * Module abi
  *
  * Provides message encoding and decoding according to the ABI specification.
  *
  * Please refer to the [[https://github.com/tonlabs/TON-SDK/blob/master/docs/mod_abi.md SDK documentation]]
  * for the detailed description of individual functions and parameters
  *
  */
// scalafmt: { maxColumn = 300 }
object Abi {

  private val module = "abi"

  final case class AbiJson(`type`: String, value: Json)
  object AbiJson {
    import io.circe.syntax._
    import io.circe.parser._
    val handle: AbiJson                                              = AbiJson("Handle", 0.asJson)
    def fromJson(abiJson: Json): AbiJson                             = AbiJson("Serialized", abiJson)
    def fromString(abiJson: String): Either[ParsingFailure, AbiJson] = parse(abiJson).map(fromJson)
    def fromResource(name: String, classLoader: ClassLoader = Thread.currentThread().getContextClassLoader): Either[ParsingFailure, AbiJson] =
      fromString(Source.fromResource(name, classLoader).mkString)
  }

  final case class StateInitParams(abi: AbiJson, value: Json)

  type MessageBodyType = String

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
    def fromMessage(source: MessageSource): StateInitSource                                                     = StateInitSource("Message", None, None, None, None, None, None, Option(source))
    def fromStateInit(code: String, data: String, library: Option[String]): StateInitSource                     = StateInitSource("StateInit", Option(code), Option(data), library, None, None, None, None)
    def fromTvc(tvc: String, public_key: Option[String], init_params: Option[StateInitParams]): StateInitSource = StateInitSource("Tvc", None, None, None, Option(tvc), public_key, init_params, None)
  }

  final case class MessageSource(`type`: String, message: Option[String], abi: Option[AbiJson], address: Option[String], deploy_set: Option[DeploySet], call_set: Option[CallSet], signer: Option[Signer], processing_try_index: Option[Int])

  object MessageSource {
    def fromEncoded(message: String, abi: Option[AbiJson]): MessageSource = MessageSource("Encoded", Option(message), abi, None, None, None, None, None)
    def fromEncodingParams(p: Request.EncodeMessage): MessageSource       = MessageSource("EncodingParams", None, Option(p.abi), p.address, p.deploy_set, p.call_set, Option(p.signer), p.processing_try_index)
  }

  object Request {
    final case class EncodeMessageBody(abi: AbiJson, call_set: CallSet, is_internal: Boolean, signer: Signer, processing_try_index: Option[Int])
    final case class AttachSignatureToMessageBody(abi: AbiJson, public_key: String, message: String, signature: String)
    final case class EncodeMessage(abi: AbiJson, address: Option[String], deploy_set: Option[DeploySet], call_set: Option[CallSet], signer: Signer, processing_try_index: Option[Int] = None)
    final case class AttachSignature(abi: AbiJson, public_key: String, message: String, signature: String)
    final case class DecodeMessage(abi: AbiJson, message: String)
    final case class DecodeMessageBody(abi: AbiJson, body: String, is_internal: Boolean)
    final case class EncodeAccount(state_init: StateInitSource, balance: Option[BigInt], last_trans_lt: Option[BigInt], last_paid: Option[BigDecimal])
    final case class EncodeInternalMessage(abi: Option[AbiJson], address: Option[String], deploy_set: Option[DeploySet], call_set: Option[CallSet], value: String, bounce: Option[Boolean], enable_ihr: Option[Boolean], src_address: Option[String] = None)
  }

  object Result {
    final case class EncodeMessageBody(body: String, data_to_sign: Option[String])
    final case class AttachSignatureToMessageBody(body: String)
    final case class EncodeMessage(message: String, data_to_sign: Option[String], address: String, message_id: String)
    final case class DecodedMessageBody(body_type: MessageBodyType, name: String, value: Option[Json], header: Option[FunctionHeader])
    final case class EncodeAccount(account: String, id: String)
    final case class AttachSignature(message: String, message_id: String)
    final case class EncodeInternalMessage(message: String, address: String, message_id: String)
  }

  import io.circe.generic.auto._

  implicit val attachSToMB           = new SdkCall[Request.AttachSignatureToMessageBody, Result.AttachSignatureToMessageBody] { override val function: String = s"$module.attach_signature_to_message_body" }
  implicit val encodeMessageBody     = new SdkCall[Request.EncodeMessageBody, Result.EncodeMessageBody]                       { override val function: String = s"$module.encode_message_body"              }
  implicit val encodeMessage         = new SdkCall[Request.EncodeMessage, Result.EncodeMessage]                               { override val function: String = s"$module.encode_message"                   }
  implicit val attachSignature       = new SdkCall[Request.AttachSignature, Result.AttachSignature]                           { override val function: String = s"$module.attach_signature"                 }
  implicit val decodeMessage         = new SdkCall[Request.DecodeMessage, Result.DecodedMessageBody]                          { override val function: String = s"$module.decode_message"                   }
  implicit val decodeMessageBody     = new SdkCall[Request.DecodeMessageBody, Result.DecodedMessageBody]                      { override val function: String = s"$module.decode_message_body"              }
  implicit val encodeAccount         = new SdkCall[Request.EncodeAccount, Result.EncodeAccount]                               { override val function: String = s"$module.encode_account"                   }
  implicit val encodeInternalMessage = new SdkCall[Request.EncodeInternalMessage, Result.EncodeInternalMessage]               { override val function: String = s"$module.encode_internal_message"          }
}
