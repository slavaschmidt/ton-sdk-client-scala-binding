package ton.sdk.client.modules

import org.scalatest._
import flatspec._
import matchers._
import ton.sdk.client.modules.Api.SdkClientError
import ton.sdk.client.modules.Context._
import ton.sdk.client.modules.Utils.Result.ConvertedAddress
import ton.sdk.client.modules.Utils._

class UtilsSpec extends AsyncFlatSpec with should.Matchers {

  behavior of "Utils"

  val accountId    = "fcb91a3a3816d0f7b8c2c76108b8a9bc5a6b7a55bd79f8ab101c52db29232260"
  val hexMainchain = "-1:fcb91a3a3816d0f7b8c2c76108b8a9bc5a6b7a55bd79f8ab101c52db29232260"
  val hexWorkchain = "0:fcb91a3a3816d0f7b8c2c76108b8a9bc5a6b7a55bd79f8ab101c52db29232260"
  val base64       = "Uf/8uRo6OBbQ97jCx2EIuKm8Wmt6Vb15+KsQHFLbKSMiYG+9"
  val base64url    = "kf_8uRo6OBbQ97jCx2EIuKm8Wmt6Vb15-KsQHFLbKSMiYIny"

  it should "not convert invalid address" in {
    val result = local { implicit ctx =>
      call(Request.ConvertAddress("this is my address", Types.accountId))
    }
    result.map(_ => fail("Should not succeed")).recover {
      case ex: SdkClientError => assert(ex.message == "Invalid address [fatal error]: this is my address")
    }
  }

  it should "convert accountId to HEX" in {
    local { implicit ctx =>
      call(Request.ConvertAddress(accountId, Types.hex))
    }.map(assertResult(ConvertedAddress(hexWorkchain)))
  }

  it should "... and back" in {
    local { implicit ctx =>
      for {
        converted <- call(Request.ConvertAddress(accountId, Types.hex))
        result <- call(Request.ConvertAddress(converted.address, Types.accountId))
      } yield result
    }.map(assertResult(ConvertedAddress(accountId)))
  }

  it should "convert hexMasterchain to base64" in {
    local { implicit ctx =>
      call(Request.ConvertAddress(hexMainchain, Types.base64()))
    }.map(assertResult(ConvertedAddress(base64)))
  }

  it should "convert base64 address to base64url" in {
    local { implicit ctx =>
      call(Request.ConvertAddress(base64, Types.base64(url = true, test = true, bounce = true)))
    }.map(assertResult(ConvertedAddress(base64url)))
  }

  it should "convert base64url address to hex" in {
    local { implicit ctx =>
      call(Request.ConvertAddress(base64url, Types.hex))
    }.map(assertResult(ConvertedAddress(hexMainchain)))
  }

}
