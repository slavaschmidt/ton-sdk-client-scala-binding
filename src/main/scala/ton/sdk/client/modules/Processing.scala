package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.modules.Api._

object Processing {

  private val prefix = "processing"

  object Request {
    case class CallSet(function_name: String, header: Option[Map[String, Json]] = None, inputs: Option[Map[String, Json]] = None)
    case class DeploySet(tvc: String, workchain_id: Int = 0, initial_data: Option[Map[String, Json]] = None)
    case class KeyPaar(public: String, secret: String)

    sealed trait Signer
    case object NoSigner                          extends Signer
    case class ExternalSigner(public_key: String) extends Signer
    case class KeysSigner(keys: KeyPaar)          extends Signer
    case class SigningBoxSigner(handler: Int)     extends Signer

    case class SendMessage(message: String, send_events: Boolean, abi: Option[Abi.Request.Abi] = None)
    case class ProcessMessage(
      abi: Abi.Request.Abi,
      signer: Signer,
      send_events: Boolean,
      address: Option[String] = None,
      deploy_set: Option[DeploySet] = None,
      call_set: Option[CallSet] = None,
      processing_try_index: Int = 0
    )

    case class ParamsOfWaitForTransaction(message: String, abi: Option[Abi.Request.Abi], shard_block_id: String, send_events: Boolean)
    case class WaitForTransaction(params: Json /*, callback: Callback*/ )
    case class ParamsOfProcessMessage(message_encode_params: String, send_events: Boolean)
  }
  object Result {
    case class SendMessage(shard_block_id: String)
    case class DecodedOutput(out_messages: Seq[Json], output: Option[Json])
    case class ResultOfProcessMessage(transaction: String, out_messages: Seq[String], decoded: Option[DecodedOutput], fees: Json)
  }

  import io.circe.generic.auto._

  implicit val sendMessage = new SdkCall[Request.SendMessage, Result.SendMessage] {
    override val functionName: String = s"$prefix.send_message"
  }

  implicit val waitForTransaction = new SdkCall[Request.WaitForTransaction, Result.ResultOfProcessMessage] {
    override val functionName: String = s"$prefix.wait_for_transaction"
  }

  implicit val processMessage = new SdkCall[Request.ProcessMessage, Result.ResultOfProcessMessage] {
    override val functionName: String = s"$prefix.process_message"
  }
}
