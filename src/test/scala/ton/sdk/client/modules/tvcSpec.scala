package ton.sdk.client.modules

import io.circe.Json
import io.circe.syntax._
import org.scalatest.flatspec.AsyncFlatSpec
import ton.sdk.client.modules.Context._
import ton.sdk.client.modules.Tvm._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AsyncTvcSpec extends TvcSpec[Future] {
  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override implicit val fe: Context.Effect[Future] = futureEffect
}
class SyncTvcSpec extends TvcSpec[Try] {
  implicit override val fe: Context.Effect[Try] = tryEffect
}
abstract class TvcSpec[T[_]] extends AsyncFlatSpec with SdkAssertions[T] {

  implicit val fe: Effect[T]

  behavior of "Tvc"

  import Tvm.runExecutor

  it should "run_executor acc_none" in {
    val message = "te6ccgEBAQEAXAAAs0gAV2lB0HI8/VEO/pBKDJJJeoOcIh+dL9JzpmRzM8PfdicAPGNEGwRWGaJsR6UYmnsFVC2llSo1ZZN5mgUnCiHf7ZaUBKgXyAAGFFhgAAAB69+UmQS/LjmiQA=="
    val result = devNet { implicit ctx =>
      val result = fe.unsafeGet(call(Request.RunExecutor(message, AccountForExecutor.none, true)))
      val parsed = call(Boc.Request.ParseAccount(result.account))
      fe.map(parsed)(p => assert(p.parsed.id === "0:f18d106c11586689b11e946269ec1550b69654a8d5964de668149c28877fb65a"&& p.parsed.acc_type_name === "Uninit"))
    }
    fe.unsafeGet(result)
  }

  // TODO test other funcitons
}
