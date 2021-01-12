package ton.sdk.client.modules

import org.scalatest._
import flatspec._
import ton.sdk.client.binding.Context
import ton.sdk.client.binding.Context._
import ton.sdk.client.modules.Utils.Result.Address
import ton.sdk.client.modules.Utils._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.Try

class SyncUtilsSpec extends UtilsSpec[Try] {
  implicit override val ef: Context.Effect[Try] = tryEffect
}

class AsyncUtilsSpec extends UtilsSpec[Future] {
  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val ef: Context.Effect[Future]         = futureEffect
}

abstract class UtilsSpec[T[_]] extends AsyncFlatSpec with SdkAssertions[T] {

  implicit val ef: Effect[T]

  behavior of "Utils"

  val accountId    = "fcb91a3a3816d0f7b8c2c76108b8a9bc5a6b7a55bd79f8ab101c52db29232260"
  val hexMainchain = "-1:fcb91a3a3816d0f7b8c2c76108b8a9bc5a6b7a55bd79f8ab101c52db29232260"
  val hexWorkchain = "0:fcb91a3a3816d0f7b8c2c76108b8a9bc5a6b7a55bd79f8ab101c52db29232260"
  val base64       = "Uf/8uRo6OBbQ97jCx2EIuKm8Wmt6Vb15+KsQHFLbKSMiYG+9"
  val base64url    = "kf_8uRo6OBbQ97jCx2EIuKm8Wmt6Vb15-KsQHFLbKSMiYIny"

  it should "not convert invalid address" in {
    val result = local { implicit ctx =>
      call(Request.ConvertAddress("this is my address", AddressStringFormat.accountId))
    }
    assertSdkError(result)("Invalid address [fatal error]: this is my address")
  }

  it should "convert accountId to HEX" in {
    val result = local { implicit ctx =>
      call(Request.ConvertAddress(accountId, AddressStringFormat.hex))
    }
    assertValue(result)(Address(hexWorkchain))
  }

  it should "convert hexMasterchain to base64" in {
    val result = local { implicit ctx =>
      call(Request.ConvertAddress(hexMainchain, AddressStringFormat.base64()))
    }
    assertValue(result)(Address(base64))
  }

  it should "convert base64 address to base64url" in {
    val result = local { implicit ctx =>
      call(Request.ConvertAddress(base64, AddressStringFormat.base64(url = true, test = true, bounce = true)))
    }
    assertValue(result)(Address(base64url))
  }

  it should "convert base64url address to hex" in {
    val result = local { implicit ctx =>
      call(Request.ConvertAddress(base64url, AddressStringFormat.hex))
    }
    assertValue(result)(Address(hexMainchain))
  }

  it should "convert it back" in {
    val result = local { implicit ctx =>
      ef.flatMap {
        call(Request.ConvertAddress(accountId, AddressStringFormat.hex))
      } { converted =>
        call(Request.ConvertAddress(converted.address, AddressStringFormat.accountId))
      }
    }
    assertValue(result)(Address(accountId))
  }

  }
