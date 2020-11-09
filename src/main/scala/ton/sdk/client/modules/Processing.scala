package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding._
import ton.sdk.client.binding.Api._

// TODO status: WIP
object Processing {

  private val prefix = "processing"

  case class ParamsOfWaitForTransaction(message: String, abi: Option[Abi.AbiJson], shard_block_id: String, send_events: Boolean)
  case class MessageEncodeParams(
    abi: Abi.AbiJson,
    signer: Signer,
    address: Option[String] = None,
    deploy_set: Option[DeploySet] = None,
    call_set: Option[CallSet] = None,
    processing_try_index: Int = 0
  )
  final case class SendMessage(message: String, abi: Option[Abi.AbiJson] = None, send_events: Boolean)
  final case class WaitForTransaction(message: String, shard_block_id: String, abi: Option[Abi.AbiJson] = None, send_events: Boolean)
  final case class ProcessMessage private (message_encode_params: MessageEncodeParams, send_events: Boolean)

  // TODO all of them support streaming
  object Request {
    def processMessage(params: MessageEncodeParams)                                            = ProcessMessage(params, false)
    def processMessageS(params: MessageEncodeParams)                                           = ProcessMessage(params, true)
    def sendMessage(message: String, abi: Option[Abi.AbiJson])                                 = SendMessage(message, abi, false)
    def sendMessageS(message: String, abi: Option[Abi.AbiJson])                                = SendMessage(message, abi, true)
    def waitForTransaction(message: String, shard_block_id: String, abi: Option[Abi.AbiJson])  = WaitForTransaction(message, shard_block_id, abi, false)
    def waitForTransactionS(message: String, shard_block_id: String, abi: Option[Abi.AbiJson]) = WaitForTransaction(message, shard_block_id, abi, true)
  }
  object Result {
    case class SendMessage(shard_block_id: String)
    case class DecodedOutput(out_messages: Seq[Json], output: Option[Json])
    case class ResultOfProcessMessage(transaction: Transaction, out_messages: Seq[String], decoded: Option[DecodedOutput], fees: Fees)
  }

  import io.circe.generic.auto._
  import Decoders.decodeCompute

  implicit val processMessage = new SdkCall[ProcessMessage, Result.ResultOfProcessMessage] {
    override val function: String = s"$prefix.process_message"
  }

  implicit val processMessageStreaming = new StreamingSdkCall[ProcessMessage, Result.ResultOfProcessMessage, Json] {
    override val function: String = s"$prefix.process_message"
  }

  implicit val sendMessage = new SdkCall[SendMessage, Result.SendMessage] {
    override val function: String = s"$prefix.send_message"
  }

  implicit val sendMessageStreaming = new StreamingSdkCall[SendMessage, Result.SendMessage, Json] {
    override val function: String = s"$prefix.send_message"
  }

  implicit val waitForTransaction = new SdkCall[WaitForTransaction, Result.ResultOfProcessMessage] {
    override val function: String = s"$prefix.wait_for_transaction"
  }

  implicit val waitForTransactionStreaming = new StreamingSdkCall[WaitForTransaction, Result.ResultOfProcessMessage, Json] {
    override val function: String = s"$prefix.wait_for_transaction"
  }

}
