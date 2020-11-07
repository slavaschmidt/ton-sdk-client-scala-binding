package ton.sdk.client.binding

import io.circe.{Decoder, HCursor, Json}
import io.circe.generic.auto._
import ton.sdk.client.modules.Abi.Abi

final case class NetworkConfig(
  server_address: String,
  network_retries_count: Option[Int] = None,
  message_retries_count: Option[Int] = None,
  message_processing_timeout: Option[Int] = None,
  wait_for_timeout: Option[Int] = None,
  out_of_sync_threshold: Option[BigInt] = None,
  access_key: Option[String] = None
)

final case class CryptoConfig(
  mnemonic_dictionary: Option[Int] = None,
  mnemonic_word_count: Option[Int] = None,
  hdkey_derivation_path: Option[String] = None,
  hdkey_compliant: Option[Boolean] = None
)

final case class AbiConfig(workchain: Option[Int], message_expiration_timeout: Option[Int], message_expiration_timeout_grow_factor: Option[Int])

final case class ClientConfig(network: Option[NetworkConfig] = None, crypto: Option[CryptoConfig] = None, abi: Option[AbiConfig] = None)

object ClientConfig {
  def fromServer(server: String): ClientConfig = ClientConfig(Option(NetworkConfig(server)))
  val mainNet                                  = fromServer("main.ton.dev")
  val devNet                                   = fromServer("net.ton.dev")
  val testNet                                  = fromServer("testnet.ton.dev")
  val local                                    = fromServer("127.0.0.1")
}

// TODO make type-safe
//case class SortDirection(direction: String)
//object SortDirection {
//  val ASC  = SortDirection("ASC")
//  val DESC = SortDirection("DESC")
//}
final case class OrderBy(path: String, direction: String)

final case class ClientResult[T](result: T)

final case class KeyPair(public: String, secret: String)

case class Fees(
  in_msg_fwd_fee: BigDecimal,
  storage_fee: BigDecimal,
  gas_fee: BigDecimal,
  out_msgs_fwd_fee: BigDecimal,
  total_account_fees: BigDecimal,
  total_output: BigDecimal
)
case class Storage(storage_fees_collected: String, status_change: Int, status_change_name: String)
case class Credit(credit: String)
sealed trait Compute {
  val compute_type: Int
  val compute_type_name: String
}
final case class ComputeVm(
  success: Boolean,
  msg_state_used: Boolean,
  account_activated: Boolean,
  gas_fees: String,
  gas_used: Double,
  gas_limit: Double,
  mode: Int,
  exit_code: Int,
  vm_steps: Int,
  vm_init_state_hash: String,
  vm_final_state_hash: String
) extends Compute {
  override val compute_type: Int         = 1
  override val compute_type_name: String = "vm"
}
final case class ComputeSkipped(
  skipped_reason: Int,
  skipped_reason_name: String
) extends Compute {
  override val compute_type: Int         = 0
  override val compute_type_name: String = "skipped"
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
  credit: Credit,
  compute: Compute,
  action: Action,
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
  out_msgs: List[String], // TODO unsure, need better tests
  account_id: String,
  total_fees: String,
  balance_delta: String,
  old_hash: String,
  new_hash: String
)

final case class CallSet(function_name: String, header: Option[Map[String, Json]] = None, inputs: Option[Map[String, Json]] = None)
final case class DeploySet(tvc: String, workchain_id: Int = 0, initial_data: Option[Map[String, Json]] = None)

final case class Signer(`type`: String, keys: Option[KeyPair] = None, public_key: Option[String] = None, handle: Option[Int] = None)

object Signer {
  val none                         = Signer("None")
  def fromKeypair(keys: KeyPair)   = Signer("Keys", keys = Option(keys))
  def fromExternal(public: String) = Signer("External", public_key = Option(public))
  def fromHandle(handle: Int)      = Signer("SigningBox", handle = Option(handle))
}

final case class FunctionHeader(expire: Option[Long], time: Option[BigInt], pubkey: Option[String])

//object Decoders {
//  implicit val decodeCompute: Decoder[Compute] = (c: HCursor) => for {
//    foo <- c.downField("compute_type").as[Int]
//    result <- foo match {
//      case 0 => Decoder[ComputeSkipped].apply(c)
//      case 1 => Decoder[ComputeVm].apply(c)
//    }
//  } yield result
//}
