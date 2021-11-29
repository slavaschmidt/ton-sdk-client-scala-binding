package ton.sdk.client.modules

import org.scalatest._
import flatspec._
import io.circe.JsonObject
import ton.sdk.client.binding.Context
import ton.sdk.client.binding.Context._
import ton.sdk.client.modules.Proofs._
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.Try

class SyncProofsSpec extends ProofsSpec[Try] {
  implicit override val ef: Context.Effect[Try] = tryEffect
}

class AsyncProofsSpec extends ProofsSpec[Future] {
  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val ef: Context.Effect[Future]         = futureEffect
}

abstract class ProofsSpec[T[_]] extends AsyncFlatSpec with SdkAssertions[T] {

  implicit val ef: Effect[T]

  behavior of "Proofs"

  it should "proof_block_data" in {
    val id = "b38d6bdb4fab0e52a9165fe65aa373520ae8c7e422f93f20c9a2a5c8016d5e7d".asJson
    val fields = """
                   |id
                   |boc
                   |account_blocks {
                   |    transactions {
                   |        lt(format:DEC) total_fees(format:DEC) total_fees_other{value(format:DEC)}
                   |    }
                   |}
                   |end_lt(format:DEC)
                   |gen_software_capabilities(format:DEC)
                   |in_msg_descr {
                   |    fwd_fee(format:DEC)
                   |    ihr_fee(format:DEC)
                   |    in_msg {fwd_fee_remaining(format:DEC)}
                   |    out_msg {fwd_fee_remaining(format:DEC)}
                   |    transit_fee(format:DEC)}
                   |master {
                   |    config {
                   |      p14 {basechain_block_fee(format:DEC) masterchain_block_fee(format:DEC)}
                   |      p17 {max_stake(format:DEC) min_stake(format:DEC) min_total_stake(format:DEC)}
                   |      p18 {
                   |        bit_price_ps(format:DEC)
                   |        cell_price_ps(format:DEC)
                   |        mc_bit_price_ps(format:DEC)
                   |        mc_cell_price_ps(format:DEC)
                   |      }
                   |      p20 {
                   |        block_gas_limit(format:DEC)
                   |        delete_due_limit(format:DEC)
                   |        flat_gas_limit(format:DEC)
                   |        flat_gas_price(format:DEC)
                   |        freeze_due_limit(format:DEC)
                   |        gas_credit(format:DEC)
                   |        gas_limit(format:DEC)
                   |        gas_price(format:DEC)
                   |        special_gas_limit(format:DEC)
                   |      }
                   |      p21 {
                   |        block_gas_limit(format:DEC)
                   |        delete_due_limit(format:DEC)
                   |        flat_gas_limit(format:DEC)
                   |        flat_gas_price(format:DEC)
                   |        freeze_due_limit(format:DEC)
                   |        gas_credit(format:DEC)
                   |        gas_limit(format:DEC)
                   |        gas_price(format:DEC)
                   |        special_gas_limit(format:DEC)
                   |      }
                   |      p24 {bit_price(format:DEC) cell_price(format:DEC) lump_price(format:DEC)}
                   |      p25 {bit_price(format:DEC) cell_price(format:DEC) lump_price(format:DEC)}
                   |      p32 {list {weight(format:DEC)} total_weight(format:DEC)}
                   |      p33 {list {weight(format:DEC)} total_weight(format:DEC)}
                   |      p34 {list {weight(format:DEC)} total_weight(format:DEC)}
                   |      p35 {list {weight(format:DEC)} total_weight(format:DEC)}
                   |      p36 {list {weight(format:DEC)} total_weight(format:DEC)}
                   |      p37 {list {weight(format:DEC)} total_weight(format:DEC)}
                   |      p8 {capabilities(format:DEC)}
                   |    }
                   |    recover_create_msg {
                   |        fwd_fee(format:DEC)
                   |        ihr_fee(format:DEC)
                   |        in_msg {fwd_fee_remaining(format:DEC)}
                   |        out_msg {fwd_fee_remaining(format:DEC)}
                   |        transit_fee(format:DEC)
                   |    }
                   |    shard_fees {
                   |        create(format:DEC)
                   |        create_other {value(format:DEC)} fees(format:DEC) fees_other {value(format:DEC)}
                   |    }
                   |    shard_hashes {
                   |        descr {
                   |            end_lt(format:DEC)
                   |            fees_collected(format:DEC)
                   |            fees_collected_other {value(format:DEC)}
                   |            funds_created(format:DEC)
                   |            funds_created_other {value(format:DEC)} start_lt(format:DEC)
                   |        }
                   |    }
                   |}
                   |master_ref {end_lt(format:DEC)}
                   |out_msg_descr {
                   |    import_block_lt(format:DEC)
                   |    imported {
                   |        fwd_fee(format:DEC)
                   |        ihr_fee(format:DEC)
                   |        in_msg {fwd_fee_remaining(format:DEC)}
                   |        out_msg {fwd_fee_remaining(format:DEC)}
                   |        transit_fee(format:DEC)
                   |    }
                   |    next_addr_pfx(format:DEC)
                   |    out_msg {fwd_fee_remaining(format:DEC)}
                   |    reimport {
                   |        fwd_fee(format:DEC)
                   |        ihr_fee(format:DEC)
                   |        in_msg {fwd_fee_remaining(format:DEC)}
                   |        out_msg {fwd_fee_remaining(format:DEC)}
                   |        transit_fee(format:DEC)
                   |    }
                   |}
                   |prev_alt_ref {end_lt(format:DEC)}
                   |prev_ref {end_lt(format:DEC)}
                   |prev_vert_alt_ref {end_lt(format:DEC)}
                   |prev_vert_ref{end_lt(format:DEC)}
                   |start_lt(format:DEC)
                   |value_flow {
                   |    created(format:DEC)
                   |    created_other {value(format:DEC)}
                   |    exported exported_other {value(format:DEC)}
                   |    fees_collected(format:DEC)
                   |    fees_collected_other {value(format:DEC)}
                   |    fees_imported(format:DEC)
                   |    fees_imported_other {value(format:DEC)}
                   |    from_prev_blk(format:DEC)
                   |    from_prev_blk_other {value(format:DEC)}
                   |    imported(format:DEC)
                   |    imported_other {value(format:DEC)}
                   |    minted(format:DEC)
                   |    minted_other {value(format:DEC)}
                   |    to_next_blk(format:DEC)
                   |    to_next_blk_other {value(format:DEC)}
                   |}""".stripMargin
    val query = Net.Request.QueryCollection("blocks", fields, filter = Some(JsonObject("id" -> JsonObject("eq" -> id).asJson).asJson), limit = Option(1))

    val result = mainNet { implicit ctx =>
      val blocks = ef.unsafeGet(call(query))
      call(Request.ProofBlockData(blocks.result.head))
    }
    assertValue(result)(())
  }

  it should "proof_transaction_data" in {
    val id = "0c7e395e8eb14c173d2dde7189200f28787a05df1fa188b19224f6e19a439dc6".asJson
    val fields = """
                   |id
                   |boc
                   |action {total_action_fees(format:DEC) total_fwd_fees(format:DEC)}
                   |balance_delta(format:DEC)
                   |balance_delta_other {value(format:DEC)}
                   |bounce {fwd_fees(format:DEC) msg_fees(format:DEC) req_fwd_fees(format:DEC)}
                   |compute {gas_fees(format:DEC) gas_limit(format:DEC) gas_used(format:DEC)}
                   |credit {credit(format:DEC) credit_other {value(format:DEC)} due_fees_collected(format:DEC)}
                   |ext_in_msg_fee(format:DEC)
                   |lt(format:DEC)
                   |prev_trans_lt(format:DEC)
                   |storage {storage_fees_collected(format:DEC) storage_fees_due(format:DEC)}
                   |total_fees(format:DEC)
                   |total_fees_other {value(format:DEC)}
                   |""".stripMargin
    val query = Net.Request.QueryCollection("transactions", fields, filter = Some(JsonObject("id" -> JsonObject("eq" -> id).asJson).asJson), limit = Option(1))

    val result = mainNet { implicit ctx =>
      val transactions = ef.unsafeGet(call(query))
      call(Request.ProofTransactionData(transactions.result.head))
    }
    assertValue(result)(())
  }

  it should "proof_message_data" in {
    val id = "4a9389e2fa34a83db0c814674bc4c7569fd3e92042289e2b2d4802231ecabec9".asJson
    val fields = """
     |id
     |boc
     |created_lt(format:DEC)
     |fwd_fee(format:DEC)
     |ihr_fee(format:DEC)
     |import_fee(format:DEC)
     |value(format:DEC)
     |value_other{value(format:DEC)}""".stripMargin
    val query = Net.Request.QueryCollection("messages", fields, filter = Some(JsonObject("id" -> JsonObject("eq" -> id).asJson).asJson), limit = Option(1))

    val result = mainNet { implicit ctx =>
      val transactions = ef.unsafeGet(call(query))
      call(Request.ProofMessageData(transactions.result.head))
    }
    assertValue(result)(())
  }
}