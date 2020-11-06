package ton.sdk.client.modules

import ton.sdk.client.modules.Api.SdkClientError
import ton.sdk.client.modules.Context._
import ton.sdk.client.modules.Utils._

import scala.concurrent.{ExecutionContext, Future}

class AsyncUtilsSpec extends UtilsSpec[Future] {

  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val fe: Context.Effect[Future]         = futureEffect

  it should "not convert invalid address" in {
    val result = local { implicit ctx =>
      call(Request.ConvertAddress("this is my address", AddressOutputFormat.accountId))
    }
    fe.map(result)(_ => fail("Should not succeed")).recover {
      case ex: SdkClientError => assert(ex.message == "Invalid address [fatal error]: this is my address")
    }
  }

  it should "convert it back" in {
    val result = local { implicit ctx =>
      val r = for {
        converted <- call(Request.ConvertAddress(accountId, AddressOutputFormat.hex))
        result    <- call(Request.ConvertAddress(converted.address, AddressOutputFormat.accountId))
      } yield result
      r
    }
    fe.unsafeGet(fe.map(result)(assertResult(Result.ConvertedAddress(accountId))))
  }

}
