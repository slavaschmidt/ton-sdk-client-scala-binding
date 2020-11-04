package ton.sdk.client.modules

import ton.sdk.client.modules.Api.SdkClientError
import ton.sdk.client.modules.Context._
import ton.sdk.client.modules.Utils.Result.ConvertedAddress
import ton.sdk.client.modules.Utils._

import scala.concurrent.{ExecutionContext, Future}

class AsyncUtilsSpec extends UtilsSpec[Future] {

  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override implicit val fe: Context.Effect[Future] = futureEffect

  it should "not convert invalid address" in {
    val result = local { implicit ctx =>
      call(Request.ConvertAddress("this is my address", Types.accountId))
    }
    fe.map(result)(_ => fail("Should not succeed")).recover {
      case ex: SdkClientError => assert(ex.message == "Invalid address [fatal error]: this is my address")
    }
  }

  it should "convert it back" in {
    val result = local { implicit ctx =>
      val r = for {
        converted <- call(Request.ConvertAddress(accountId, Types.hex))
        result <- call(Request.ConvertAddress(converted.address, Types.accountId))
      } yield result
      r
    }
    fe.unsafeGet(fe.map(result)(assertResult(ConvertedAddress(accountId))))
  }

}
