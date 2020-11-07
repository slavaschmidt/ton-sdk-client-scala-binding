package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding.Transaction
import ton.sdk.client.modules.Api.SdkCall

object Boc {

  private val prefix = "boc"

  final case class Message(
    json_version: Int,
    id: String,
    boc: String,
    status: Int,
    status_name: String,
    msg_type: Int,
    msg_type_name: String,
    src: String,
    src_workchain_id: Int,
    dst: String,
    dst_workchain_id: Int,
    ihr_disabled: Boolean,
    ihr_fee: String,
    fwd_fee: String,
    bounce: Boolean,
    bounced: Boolean,
    value: String,
    created_lt: String,
    created_at: Long,
    body: String // TODO was absent
  )


  case class Account(
    json_version: Int,
    id: String,
    workchain_id: Int,
    boc: String,
    last_paid: BigDecimal,
    last_trans_lt: String,
    balance: String,
    code: Option[String],
    code_hash: Option[String],
    data: Option[String],
    data_hash: Option[String],
    acc_type: Int,
    acc_type_name: String
  )

  case class MasterRef(
    end_lt: String,
    seq_no: Long,
    root_hash: String,
    file_hash: String
  )
  case class ValueFlow(
    from_prev_blk: String,
    to_next_blk: String,
    imported: String,
    exported: String,
    fees_collected: String,
    fees_imported: String,
    recovered: String,
    created: String,
    minted: String
  )
  case class InMsgDescr(
    msg_id: String,
    transaction_id: String,
    msg_type: Int,
    msg_type_name: String
  )
  case class OutMsg(
    msg_id: String,
    cur_addr: String,
    next_addr: String,
    fwd_fee_remaining: String
  )
  case class OutMsgDescr(
    out_msg: OutMsg,
    transaction_id: String,
    msg_type: Int,
    msg_type_name: String
  )
  case class Transactions(
    lt: String,
    transaction_id: String,
    total_fees: String
  )
  case class AccountBlocks(
    account_addr: String,
    transactions: List[Transactions],
    old_hash: String,
    new_hash: String,
    tr_count: Long
  )
  case class Block(
    json_version: Int,
    id: String,
    status: Int,
    status_name: String,
    boc: String,
    global_id: Long,
    version: Int,
    after_merge: Boolean,
    before_split: Boolean,
    after_split: Boolean,
    want_split: Boolean,
    want_merge: Boolean,
    key_block: Boolean,
    vert_seqno_incr: Int,
    seq_no: Long,
    vert_seq_no: Long,
    gen_utime: Long,
    start_lt: String,
    end_lt: String,
    gen_validator_list_hash_short: Long,
    gen_catchain_seqno: Long,
    min_ref_mc_seqno: Long,
    prev_key_block_seqno: Long,
    workchain_id: Int,
    shard: String,
    gen_software_version: Int,
    gen_software_capabilities: String,
    prev_seq_no: Long,
    master_ref: MasterRef,
    prev_ref: MasterRef,
    value_flow: ValueFlow,
    old_hash: String,
    new_hash: String,
    old_depth: Long,
    new_depth: Long,
    in_msg_descr: List[InMsgDescr],
    out_msg_descr: List[OutMsgDescr],
    account_blocks: List[AccountBlocks],
    tr_count: Long,
    rand_seed: String,
    created_by: String
  )

  object Request {
    case class ParseMessage(boc: String)
    case class ParseTransaction(boc: String)
    case class ParseAccount(boc: String)
    case class ParseBlock(boc: String)
    case class GetBlockchainConfig(block_boc: String)
  }
  object Result {
    case class Parsed[T](parsed: T)
    case class ConfigBoc(config_boc: String)
  }

  import io.circe.generic.auto._

  implicit val parseMessage = new SdkCall[Request.ParseMessage, Result.Parsed[Message]] {
    override val functionName: String = s"$prefix.parse_message"
  }
  implicit val parseTransaction = new SdkCall[Request.ParseTransaction, Result.Parsed[Transaction]] {
    override val functionName: String = s"$prefix.parse_transaction"
  }
  implicit val parseAccount = new SdkCall[Request.ParseAccount, Result.Parsed[Account]] {
    override val functionName: String = s"$prefix.parse_account"
  }
  implicit val parseBlock = new SdkCall[Request.ParseBlock, Result.Parsed[Block]] {
    override val functionName: String = s"$prefix.parse_block"
  }
  implicit val getBlockchainConfig = new SdkCall[Request.GetBlockchainConfig, Result.ConfigBoc] {
    override val functionName: String = s"$prefix.get_blockchain_config"
  }

}
