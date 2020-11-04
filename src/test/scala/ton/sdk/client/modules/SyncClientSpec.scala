package ton.sdk.client.modules

import ton.sdk.client.modules.Client.Request
import ton.sdk.client.modules.Context.{call, local, tryEffect}

import scala.util.{Success, Try}

class SyncClientSpec extends ClientSpec[Try] {
  implicit override val fe: Context.Effect[Try] = tryEffect

  it should "be able to do stuff in sequentially in single context and in multiple contexts" in {
    val r1 = local { implicit ctx =>
      Try { for (_ <- 1 to 10) yield call(Request.ApiReference) }
    }.get
    val r2 = for (_ <- 1 to 10) yield local { implicit ctx =>
      call(Request.ApiReference)
    }
    val result1 = r1.collect { case Success(s) => s }
    val result2 = r2.collect { case Success(s) => s }
    assert(result1 == result2 && result1.size == 10)
    succeed
  }

}
