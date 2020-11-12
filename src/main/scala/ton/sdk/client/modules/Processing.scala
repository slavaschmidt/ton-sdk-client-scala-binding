package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding._
import ton.sdk.client.binding.Api._

object Processing {

  private val module = "processing"

  case class MessageEncodeParams(
    abi: Abi.AbiJson,
    signer: Signer,
    address: Option[String] = None,
    deploy_set: Option[DeploySet] = None,
    call_set: Option[CallSet] = None,
    processing_try_index: Int = 0
  )
  abstract class WaitForTransaction(val message: String, val shard_block_id: String, val abi: Option[Abi.AbiJson], val send_events: Boolean)
  abstract class SendMessage(val message: String, val abi: Option[Abi.AbiJson], val send_events: Boolean)
  abstract class ProcessMessage(val message_encode_params: MessageEncodeParams, val send_events: Boolean)

  object Request {
    final case class ProcessMessageWithEvents(override val message_encode_params: MessageEncodeParams)                    extends ProcessMessage(message_encode_params, true)
    final case class ProcessMessageWithoutEvents(override val message_encode_params: MessageEncodeParams)                 extends ProcessMessage(message_encode_params, false)
    final case class SendMessageWithEvents(override val message: String, override val abi: Option[Abi.AbiJson] = None)    extends SendMessage(message, abi, true)
    final case class SendMessageWithoutEvents(override val message: String, override val abi: Option[Abi.AbiJson] = None) extends SendMessage(message, abi, false)

    final case class WaitForTransactionWithEvents(override val message: String, override val shard_block_id: String, override val abi: Option[Abi.AbiJson] = None)
        extends WaitForTransaction(message, shard_block_id, abi, true)
    final case class WaitForTransactionWithoutEvents(override val message: String, override val shard_block_id: String, override val abi: Option[Abi.AbiJson] = None)
        extends WaitForTransaction(message, shard_block_id, abi, false)
  }
  object Result {
    case class ResultOfSendMessage(shard_block_id: String)
    case class ResultOfProcessMessage(transaction: Transaction, out_messages: Seq[String], decoded: Option[DecodedOutput], fees: Fees)
  }

  import io.circe.generic.auto._
  import Decoders._

  implicit val processMessage = new SdkCall[Request.ProcessMessageWithoutEvents, Result.ResultOfProcessMessage] {
    override val function: String = s"$module.process_message"
  }

  implicit val processMessageStreaming = new StreamingSdkCall[Request.ProcessMessageWithEvents, Result.ResultOfProcessMessage, Json] {
    override val function: String = s"$module.process_message"
  }

  implicit val sendMessage = new SdkCall[Request.SendMessageWithoutEvents, Result.ResultOfSendMessage] {
    override val function: String = s"$module.send_message"
  }

  implicit val sendMessageStreaming = new StreamingSdkCall[Request.SendMessageWithEvents, Result.ResultOfSendMessage, Json] {
    override val function: String = s"$module.send_message"
  }

  implicit val waitForTransaction = new SdkCall[Request.WaitForTransactionWithoutEvents, Result.ResultOfProcessMessage] {
    override val function: String = s"$module.wait_for_transaction"
  }

  implicit val waitForTransactionStreaming = new StreamingSdkCall[Request.WaitForTransactionWithEvents, Result.ResultOfProcessMessage, Json] {
    override val function: String = s"$module.wait_for_transaction"
  }

}
