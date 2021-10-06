package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding.Transaction
import ton.sdk.client.binding.Api.SdkCall

/**
  * Module boc
  *
  * BOC manipulation module.
  *
  * Please refer to the [[https://github.com/tonlabs/TON-SDK/blob/master/docs/mod_boc.md SDK documentation]]
  * for the detailed description of individual functions and parameters
  *
  */
// scalafmt: { maxColumn = 300 }
object Boc {

  private val module = "boc"

  final case class Message(
    json_version: Int,
    id: String,
    boc: String,
    status: Int,
    status_name: String,
    msg_type: Int,
    msg_type_name: String,
    src: String,
    src_workchain_id: Option[Int],
    dst: String,
    dst_workchain_id: Int,
    ihr_disabled: Option[Boolean],
    ihr_fee: Option[String],
    fwd_fee: Option[String],
    bounce: Option[Boolean],
    bounced: Option[Boolean],
    value: Option[String],
    created_lt: Option[String],
    created_at: Option[Long],
    body: Option[String],
    body_hash: Option[String],
    import_fee: Option[String]
  )

  final case class Account(
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

  final case class MasterRef(
    end_lt: String,
    seq_no: Long,
    root_hash: String,
    file_hash: String
  )
  final case class ValueFlow(
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
  final case class InMsgDescr(
    msg_id: String,
    transaction_id: String,
    msg_type: Int,
    msg_type_name: String
  )
  final case class OutMsg(
    msg_id: String,
    cur_addr: String,
    next_addr: String,
    fwd_fee_remaining: String
  )
  final case class OutMsgDescr(
    out_msg: OutMsg,
    transaction_id: String,
    msg_type: Int,
    msg_type_name: String
  )
  final case class Transactions(
    lt: String,
    transaction_id: String,
    total_fees: String
  )
  final case class AccountBlocks(
    account_addr: String,
    transactions: List[Transactions],
    old_hash: String,
    new_hash: String,
    tr_count: Long
  )
  final case class Block(
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

  final case class ShardState(id: String, workchain_id: Int, seq_no: Long)
  final case class BocCacheType(`type`: String, pin: Option[String])
  def cacheTypePinned(pin: String) = BocCacheType("Pinned", Option(pin))
  val cacheTypeUnpinned            = BocCacheType("Unpinned", None)

  final case class BuilderOp(`type`: String, size: Option[Int] = None, value: Option[Json] = None, builder: Option[Seq[BuilderOp]] = None, boc: Option[String] = None)

  object BuilderOp {
    def integer(size: Int, value: Json)        = BuilderOp("Integer", size = Option(size), value = Option(value))
    def bitString(value: String): BuilderOp    = BuilderOp("BitString", value = Option(Json.fromString(value)))
    def cell(value: Seq[BuilderOp]): BuilderOp = BuilderOp("Cell", builder = Option(value))
    def boc(value: String)                     = BuilderOp("CellBoc", boc = Option(value))

    def b(value: Byte): BuilderOp              = integer(1, Json.fromInt(value.intValue()))
    def u128(value: BigInt): BuilderOp         = integer(128, Json.fromBigInt(value))
    def u8(value: Long): BuilderOp             = integer(8, Json.fromLong(value))
    def i8(value: Long): BuilderOp             = u8(value)
    def i(size: Int, value: Number): BuilderOp = integer(size, Json.fromLong(value.longValue()))
  }

  object Request {
    final case class ParseMessage(boc: String)
    final case class ParseTransaction(boc: String)
    final case class ParseAccount(boc: String)
    final case class ParseBlock(boc: String)
    final case class GetBlockchainConfig(block_boc: String)
    final case class ParseShardstate(boc: String, id: String, workchain_id: Int)
    final case class GetBocHash(boc: String)
    final case class GetCodeFromTvc(tvc: String)
    final case class CacheGet(boc_ref: String)
    final case class CacheSet(boc: String, cache_type: BocCacheType)
    final case class CacheUnpin(pin: String, boc_ref: Option[String])
    final case class EncodeBoc(builder: Seq[BuilderOp], boc_cache: Option[BocCacheType])
    final case class GetCodeSalt(code: String, boc_cache: Option[BocCacheType])
    final case class SetCodeSalt(code: String, salt: String, boc_cache: Option[BocCacheType])
    final case class EncodeTvc(code: Option[String], data: Option[String], library: Option[String], tick: Option[Boolean], tock: Option[Boolean], split_depth: Option[Int], boc_cache: Option[BocCacheType])
    final case class DecodeTvc(tvc: String, boc_cache: Option[BocCacheType])
    final case class GetCompilerVersion(code: String)
  }
  object Result {
    final case class Parsed[T](parsed: T)
    final case class ConfigBoc(config_boc: String)
    final case class BocHash(hash: String)
    final case class CodeFromTvc(code: String)
    final case class CacheGet(boc: Option[String])
    final case class CacheSet(boc_ref: String)
    final case class EncodedBoc(boc: String)
    final case class CodeSaltSalt(salt: Option[String])
    final case class CodeSaltCode(code: String)
    final case class DecodedTvc(code: Option[String], data: Option[String], library: Option[String], tick: Option[Boolean], tock: Option[Boolean], split_depth: Option[Int])
    final case class EncodedTvc(tvc: String)
    final case class CompilerVersion(version: Option[String])
  }

  import io.circe.generic.auto._
  import ton.sdk.client.binding.Decoders.decodeCompute

  implicit val parseMessage        = new SdkCall[Request.ParseMessage, Result.Parsed[Message]]         { override val function: String = s"$module.parse_message"         }
  implicit val parseTransaction    = new SdkCall[Request.ParseTransaction, Result.Parsed[Transaction]] { override val function: String = s"$module.parse_transaction"     }
  implicit val parseAccount        = new SdkCall[Request.ParseAccount, Result.Parsed[Account]]         { override val function: String = s"$module.parse_account"         }
  implicit val parseBlock          = new SdkCall[Request.ParseBlock, Result.Parsed[Block]]             { override val function: String = s"$module.parse_block"           }
  implicit val getBlockchainConfig = new SdkCall[Request.GetBlockchainConfig, Result.ConfigBoc]        { override val function: String = s"$module.get_blockchain_config" }
  implicit val parseShardstate     = new SdkCall[Request.ParseShardstate, Result.Parsed[ShardState]]   { override val function: String = s"$module.parse_shardstate"      }
  implicit val getBocHash          = new SdkCall[Request.GetBocHash, Result.BocHash]                   { override val function: String = s"$module.get_boc_hash"          }
  implicit val getCodeFromTvc      = new SdkCall[Request.GetCodeFromTvc, Result.CodeFromTvc]           { override val function: String = s"$module.get_code_from_tvc"     }
  implicit val cacheSet            = new SdkCall[Request.CacheSet, Result.CacheSet]                    { override val function: String = s"$module.cache_set"             }
  implicit val cacheGet            = new SdkCall[Request.CacheGet, Result.CacheGet]                    { override val function: String = s"$module.cache_get"             }
  implicit val cacheUnpin          = new SdkCall[Request.CacheUnpin, Unit]                             { override val function: String = s"$module.cache_unpin"           }
  implicit val encodeBoc           = new SdkCall[Request.EncodeBoc, Result.EncodedBoc]                 { override val function: String = s"$module.encode_boc"            }
  implicit val getCodeSalt         = new SdkCall[Request.GetCodeSalt, Result.CodeSaltSalt]             { override val function: String = s"$module.get_code_salt"         }
  implicit val setCodeSalt         = new SdkCall[Request.SetCodeSalt, Result.CodeSaltCode]             { override val function: String = s"$module.set_code_salt"         }
  implicit val encodeTvc           = new SdkCall[Request.EncodeTvc, Result.EncodedTvc]                 { override val function: String = s"$module.encode_tvc"            }
  implicit val decodeTvc           = new SdkCall[Request.DecodeTvc, Result.DecodedTvc]                 { override val function: String = s"$module.decode_tvc"            }
  implicit val getCompilerVersion  = new SdkCall[Request.GetCompilerVersion, Result.CompilerVersion]   { override val function: String = s"$module.get_compiler_version"  }

}
