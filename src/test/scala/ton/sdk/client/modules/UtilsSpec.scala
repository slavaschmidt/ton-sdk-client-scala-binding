package ton.sdk.client.modules

import org.scalatest._
import flatspec._
import matchers._
import ton.sdk.client.modules.Context._
import ton.sdk.client.modules.Utils.Result.ConvertedAddress
import ton.sdk.client.modules.Utils._

abstract class UtilsSpec[T[_]] extends AsyncFlatSpec with should.Matchers {

  implicit val fe: Effect[T]

  behavior of "Utils"

  val accountId    = "fcb91a3a3816d0f7b8c2c76108b8a9bc5a6b7a55bd79f8ab101c52db29232260"
  val hexMainchain = "-1:fcb91a3a3816d0f7b8c2c76108b8a9bc5a6b7a55bd79f8ab101c52db29232260"
  val hexWorkchain = "0:fcb91a3a3816d0f7b8c2c76108b8a9bc5a6b7a55bd79f8ab101c52db29232260"
  val base64       = "Uf/8uRo6OBbQ97jCx2EIuKm8Wmt6Vb15+KsQHFLbKSMiYG+9"
  val base64url    = "kf_8uRo6OBbQ97jCx2EIuKm8Wmt6Vb15-KsQHFLbKSMiYIny"

  it should "convert accountId to HEX" in {
    val result = local { implicit ctx =>
      call(Request.ConvertAddress(accountId, AddressOutputFormat.hex))
    }
    fe.unsafeGet(fe.map(result)(assertResult(ConvertedAddress(hexWorkchain))))
  }

  it should "convert hexMasterchain to base64" in {
    val result = local { implicit ctx =>
      call(Request.ConvertAddress(hexMainchain, AddressOutputFormat.base64()))
    }
    fe.unsafeGet(fe.map(result)(assertResult(ConvertedAddress(base64))))
  }

  it should "convert base64 address to base64url" in {
    val result = local { implicit ctx =>
      call(Request.ConvertAddress(base64, AddressOutputFormat.base64(url = true, test = true, bounce = true)))
    }
    fe.unsafeGet(fe.map(result)(assertResult(ConvertedAddress(base64url))))
  }

  it should "convert base64url address to hex" in {
    val result = local { implicit ctx =>
      call(Request.ConvertAddress(base64url, AddressOutputFormat.hex))
    }
    fe.unsafeGet(fe.map(result)(assertResult(ConvertedAddress(hexMainchain))))
  }

}
