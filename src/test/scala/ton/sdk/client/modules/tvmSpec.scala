package ton.sdk.client.modules


import org.scalatest.flatspec.AsyncFlatSpec
import ton.sdk.client.modules.Context._
import ton.sdk.client.modules.Tvm._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AsyncTvmSpec extends TvmSpec[Future] {
  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override implicit val ef: Context.Effect[Future] = futureEffect
}
class SyncTvmSpec extends TvmSpec[Try] {
  implicit override val ef: Context.Effect[Try] = tryEffect
}
abstract class TvmSpec[T[_]] extends AsyncFlatSpec with SdkAssertions[T] {

  implicit val ef: Effect[T]

  behavior of "Tvm"

  it should "run_executor acc_none" in {
    val message = "te6ccgEBAQEAXAAAs0gAV2lB0HI8/VEO/pBKDJJJeoOcIh+dL9JzpmRzM8PfdicAPGNEGwRWGaJsR6UYmnsFVC2llSo1ZZN5mgUnCiHf7ZaUBKgXyAAGFFhgAAAB69+UmQS/LjmiQA=="
    val result = devNet { implicit ctx =>
      ef.flatMap(call(Request.RunExecutor(message, AccountForExecutor.none, true))) { result =>
        call(Boc.Request.ParseAccount(result.account))
      }
    }
    assertExpression(result)(p => p.parsed.id === "0:f18d106c11586689b11e946269ec1550b69654a8d5964de668149c28877fb65a"&& p.parsed.acc_type_name === "Uninit")
  }

//  it should "decode transaction" in {
//    import io.circe.generic.auto._
//    import io.circe.jawn.decode
//    val transactionJson = """{"json_version":4,"id":"cc867724b494108cbfa93b0a35777b785815dbeb62e790729f4c859c4cf91203","boc":"te6ccgECBQEAAQsAA69/GNEGwRWGaJsR6UYmnsFVC2llSo1ZZN5mgUnCiHf7ZaAAAAAAAPQkEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAX6ZtmQABgIAwIBABEMCUBKgXyAASAAgnKQrsiWWvq7FuvDy5tAjrrnG2GNeHiLyA0JhDWTysmNpEJ3xkQWYEuQL+0qt2w/d5aGNxglbLhnwfGKcsLlxxmZAQGgBACzSABXaUHQcjz9UQ7+kEoMkkl6g5wiH50v0nOmZHMzw992JwA8Y0QbBFYZomxHpRiaewVULaWVKjVlk3maBScKId/tlpQEqBfIAAYUWGAAAAHr35SZBL8uOaJA","status":3,"status_name":"finalized","storage":{"storage_fees_collected":"0x0","status_change":0,"status_change_name":"unchanged"},"credit":{"credit":"0x12a05f200"},"compute":{"skipped_reason":0,"skipped_reason_name":"noState","compute_type":0,"compute_type_name":"skipped"},"credit_first":true,"aborted":true,"destroyed":false,"tr_type":0,"tr_type_name":"ordinary","lt":"0xf4241","prev_trans_hash":"0000000000000000000000000000000000000000000000000000000000000000","prev_trans_lt":"0x0","now":1604742553,"outmsg_cnt":0,"orig_status":3,"orig_status_name":"NonExist","end_status":0,"end_status_name":"Uninit","in_msg":"08c17a8de3ace88058e0aa6f881c7ba9c5c49ab4817a90c29932cd6a163b095b","out_msgs":[],"account_id":"f18d106c11586689b11e946269ec1550b69654a8d5964de668149c28877fb65a","total_fees":"0x0","balance_delta":"0x12a05f200","old_hash":"90aec8965afabb16ebc3cb9b408ebae71b618d78788bc80d09843593cac98da4","new_hash":"4277c64416604b902fed2ab76c3f7796863718256cb867c1f18a72c2e5c71999"},"out_messages":[],"decoded":null,"account":"te6ccgEBAQEAOQAAbcAPGNEGwRWGaJsR6UYmnsFVC2llSo1ZZN5mgUnCiHf7ZaICW8L9M2zIAAAAAAA9CQlASoF8gAQ=","fees":{"in_msg_fwd_fee":0,"storage_fee":0,"gas_fee":0,"out_msgs_fwd_fee":0,"total_account_fees":0,"total_output":0}"""
//    val t = decode[Transaction](transactionJson)
//    println(t)
//    assert(t.isRight)
//  }
}
