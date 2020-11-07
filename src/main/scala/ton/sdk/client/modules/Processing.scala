package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding.{CallSet, DeploySet, KeyPair, Signer}
import ton.sdk.client.modules.Api._


// TODO status: WIP
object Processing {

  private val prefix = "processing"

  case class ParamsOfWaitForTransaction(message: String, abi: Option[Abi.Abi], shard_block_id: String, send_events: Boolean)
  case class ParamsOfProcessMessage(message_encode_params: String, send_events: Boolean)

  // TODO all of them support streaming
  object Request {
    final case class SendMessage(message: String, send_events: Boolean, abi: Option[Abi.Abi] = None) // with callback
    final case class WaitForTransaction(message: String, send_events: Boolean, shard_block_id: String, abi: Option[Abi.Abi] = None) // with callback
    final case class ProcessMessage(abi: Abi.Abi, signer: Signer, send_events: Boolean, address: Option[String] = None, deploy_set: Option[DeploySet] = None, call_set: Option[CallSet] = None, processing_try_index: Int = 0)
  }
  object Result {
    case class SendMessage(shard_block_id: String) // TODO
    case class DecodedOutput(out_messages: Seq[Json], output: Option[Json])
    case class ResultOfProcessMessage(transaction: String, out_messages: Seq[String], decoded: Option[DecodedOutput], fees: Json)
  }

  import io.circe.generic.auto._

  implicit val sendMessage = new SdkCall[Request.SendMessage, Result.SendMessage] {
    override val function: String = s"$prefix.send_message"
  }

  implicit val waitForTransaction = new SdkCall[Request.WaitForTransaction, Result.ResultOfProcessMessage] {
    override val function: String = s"$prefix.wait_for_transaction"
  }

  implicit val processMessage = new SdkCall[Request.ProcessMessage, Result.ResultOfProcessMessage] {
    override val function: String = s"$prefix.process_message"
  }
}
