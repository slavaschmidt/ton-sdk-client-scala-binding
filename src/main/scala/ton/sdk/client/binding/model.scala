package ton.sdk.client.binding

import io.circe.generic.auto._
import io.circe.{Decoder, Encoder, HCursor, Json}
import ton.sdk.client.modules.Processing

/**
  *  Collection of common types and codecs shared by multiple modules or used by the client itself.
  *  Also defines some case class based refinements over types represented in SDK client to loosely.
  */
/**
  * Network config.
  */
final case class NetworkConfig(
  server_address: Option[String],
  endpoints: Seq[String],
  network_retries_count: Option[Int] = None,
  max_reconnect_timeout: Option[Int] = None,
  message_retries_count: Option[Int] = None,
  message_processing_timeout: Option[Int] = None,
  wait_for_timeout: Option[Int] = None,
  out_of_sync_threshold: Option[BigInt] = None,
  reconnect_timeout: Option[Int] = None,
  access_key: Option[String] = None,
  queries_protocol: Option[String] = None,
  first_remp_status_timeout: Option[Int] = None,
  next_remp_status_timeout: Option[Int] = None
)

final case class CryptoConfig(
  mnemonic_dictionary: Option[Int] = None,
  mnemonic_word_count: Option[Int] = None,
  hdkey_derivation_path: Option[String] = None,
  hdkey_compliant: Option[Boolean] = None
)

final case class AbiConfig(workchain: Option[Int], message_expiration_timeout: Option[Int], message_expiration_timeout_grow_factor: Option[Int])

final case class ClientConfig(network: Option[NetworkConfig] = None, crypto: Option[CryptoConfig] = None, abi: Option[AbiConfig] = None)

/**
  * Collection of known networks.
  */
object ClientConfig {
  def fromServer(enpoints: String*): ClientConfig = ClientConfig(Option(NetworkConfig(None, enpoints)))

  val MAIN_NET = fromServer(
    "https://eri01.main.everos.dev",
    "https://gra01.main.everos.dev",
    "https://gra02.main.everos.dev",
    "https://lim01.main.everos.dev",
    "https://rbx01.main.everos.dev"
  )
  val DEV_NET  = fromServer("https://eri01.net.everos.dev", "https://rbx01.net.everos.dev", "https://gra01.net.everos.dev")
  val TEST_NET = fromServer("testnet.ton.dev")
  val LOCAL    = fromServer("http://0.0.0.0/", "http://127.0.0.1/", " http://localhost/")
}

// TODO make type-safe like this:
//case class SortDirection(direction: String)
//object SortDirection {
//  val ASC  = SortDirection("ASC")
//  val DESC = SortDirection("DESC")
//}
final case class OrderBy(path: String, direction: String)

final case class ClientResult[T](result: T)

final case class KeyPair(public: String, secret: String)

case class TransactionFees(
  in_msg_fwd_fee: BigInt,
  storage_fee: BigInt,
  gas_fee: BigInt,
  out_msgs_fwd_fee: BigInt,
  total_account_fees: BigInt,
  total_output: BigInt
)

case class Storage(storage_fees_collected: String, status_change: Int, status_change_name: String)
case class Credit(credit: String)
sealed trait Compute {
  val compute_type: Int
  val compute_type_name: String
}
object Compute {
  def vm(
    success: Boolean,
    msg_state_used: Boolean,
    account_activated: Boolean,
    gas_fees: String,
    gas_used: BigDecimal,
    gas_limit: BigDecimal,
    mode: Int,
    exit_code: Int,
    vm_steps: Int,
    vm_init_state_hash: String,
    vm_final_state_hash: String
  ) = ComputeVm(success, msg_state_used, account_activated, gas_fees, gas_used, gas_limit, mode, exit_code, vm_steps, vm_init_state_hash, vm_final_state_hash)
}
final case class ComputeVm(
  success: Boolean,
  msg_state_used: Boolean,
  account_activated: Boolean,
  gas_fees: String,
  gas_used: BigDecimal,
  gas_limit: BigDecimal,
  mode: Int,
  exit_code: Int,
  vm_steps: Int,
  vm_init_state_hash: String,
  vm_final_state_hash: String
) extends Compute {
  override val compute_type: Int = 1
  override val compute_type_name = "vm"
}

final case class ComputeSkipped(skipped_reason: Int, skipped_reason_name: String) extends Compute {
  override val compute_type: Int = 0
  override val compute_type_name = "skipped"
}

case class Action(
  success: Boolean,
  valid: Boolean,
  no_funds: Boolean,
  status_change: Int,
  result_code: Int,
  tot_actions: Int,
  spec_actions: Int,
  skipped_actions: Int,
  msgs_created: Int,
  action_list_hash: String,
  tot_msg_size_cells: Long,
  tot_msg_size_bits: Long
)
case class Transaction(
  json_version: Int,
  id: String,
  boc: String,
  status: Int,
  status_name: String,
  storage: Storage,
  credit: Option[Credit],
  compute: Compute,
  action: Option[Action],
  credit_first: Boolean,
  aborted: Boolean,
  destroyed: Boolean,
  tr_type: Int,
  tr_type_name: String,
  lt: String,
  prev_trans_hash: String,
  prev_trans_lt: String,
  now: Int,
  outmsg_cnt: Int,
  orig_status: Int,
  orig_status_name: String,
  end_status: Int,
  end_status_name: String,
  in_msg: String,
  out_msgs: Seq[String], // TODO double-check as the Transaction is just Any in SDK definition
  account_id: Option[String],
  account_addr: Option[String],
  total_fees: String,
  balance_delta: String,
  old_hash: String,
  new_hash: String
)

final case class CallSet(function_name: String, header: Option[Map[String, Json]] = None, input: Option[Map[String, Json]] = None)
final case class DeploySet(tvc: String, workchain_id: Int = 0, initial_data: Option[Map[String, Json]] = None, initial_pubkey: Option[String] = None)
final case class Signer(`type`: String, keys: Option[KeyPair] = None, public_key: Option[String] = None, handle: Option[Int] = None)

object Signer {
  val none                         = Signer("None")
  def fromKeypair(keys: KeyPair)   = Signer("Keys", keys = Option(keys))
  def fromExternal(public: String) = Signer("External", public_key = Option(public))
  def fromHandle(handle: Int)      = Signer("SigningBox", handle = Option(handle))
}

final case class FunctionHeader(expire: Option[Long], time: Option[BigInt], pubkey: Option[String])

final case class Handle(handle: Long)

final case class FetchNextBlockMessage(`type`: String, shard_block_id: String, message_id: String, message: String)

final case class DecodedOutput(out_messages: Seq[Json], output: Option[Json])

/**
  * Overrides for circe decoders where defaults are not match SDK representation
  */
object Decoders {
  implicit val decodeCompute: Decoder[Compute] = (c: HCursor) =>
    for {
      foo <- c.downField("compute_type").as[Int]
      result <- foo match {
        case 0 => Decoder[ComputeSkipped].apply(c)
        case 1 => Decoder[ComputeVm].apply(c)
      }
    } yield result

  implicit val encodeSendMessageWith: Encoder[Processing.Request.SendMessageWithEvents] =
    Encoder.forProduct3("message", "abi", "send_events")(u => (u.message, u.abi, u.send_events))
  implicit val encodeSendMessageWithout: Encoder[Processing.Request.SendMessageWithoutEvents] =
    Encoder.forProduct3("message", "abi", "send_events")(u => (u.message, u.abi, u.send_events))
  implicit val encodeWaitForTransWithout: Encoder[Processing.Request.WaitForTransactionWithoutEvents] =
    Encoder.forProduct4("message", "shard_block_id", "abi", "send_events")(u => (u.message, u.shard_block_id, u.abi, u.send_events))
  implicit val encodeWaitForTransWith: Encoder[Processing.Request.WaitForTransactionWithEvents] =
    Encoder.forProduct4("message", "shard_block_id", "abi", "send_events")(u => (u.message, u.shard_block_id, u.abi, u.send_events))
  implicit val encodeProcessMessageWith: Encoder[Processing.Request.ProcessMessageWithEvents] =
    Encoder.forProduct2("message_encode_params", "send_events")(u => (u.message_encode_params, u.send_events))
  implicit val encodeProcessMessageWithout: Encoder[Processing.Request.ProcessMessageWithoutEvents] =
    Encoder.forProduct2("message_encode_params", "send_events")(u => (u.message_encode_params, u.send_events))
//  implicit def encodeDebotExecute[D]: Encoder[Debot.Request.Execute[D]] =
//    Encoder.forProduct2("debot_handle", "action")(u => (u.debot_handle, u.action))
}

trait BaseAppCallback {
  def tpe: String = getClass.getSimpleName
}
