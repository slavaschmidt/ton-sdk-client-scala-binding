package ton.sdk.client.modules

import org.scalatest._
import ton.sdk.client.modules.Api.SdkClientError
import ton.sdk.client.modules.Context._
import ton.sdk.client.modules.Utils.Result.ConvertedAddress
import ton.sdk.client.modules.Utils._

import scala.util.Try

class SyncUtilsSpec extends UtilsSpec[Try] {

  override implicit val fe: Context.Effect[Try] = tryEffect

  implicit def tryToAssertion(t: Try[Assertion]) = Context.fromTry(t)

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
      for {
        converted <- call(Request.ConvertAddress(accountId, AddressOutputFormat.hex))
        result <- call(Request.ConvertAddress(converted.address, AddressOutputFormat.accountId))
      } yield result
    }
    fe.unsafeGet(fe.map(result)(assertResult(ConvertedAddress(accountId))))
  }

}
