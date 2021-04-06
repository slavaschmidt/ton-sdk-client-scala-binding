package ton.sdk.client.modules

import org.scalatest._
import flatspec._
import ton.sdk.client.binding.Context
import ton.sdk.client.binding.Context._
import ton.sdk.client.modules.Utils.Result.Address
import ton.sdk.client.modules.Utils._

import java.util.Base64
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

  it should "calculate fee" in {
    val result = local { implicit ctx =>
      call(
        Request.CalcStorageFee(
          "te6ccgECHQEAA/wAAnfAArtKDoOR5+qId/SCUGSSS9Qc4RD86X6TnTMjmZ4e+7EyOobmQvsHNngAAAg6t/34DgJWKJuuOehjU0ADAQFBlcBqp0PR+QAN1kt1SY8QavS350RCNNfeZ+ommI9hgd/gAgBToB6t2E3E7a7aW2YkvXv2hTmSWVRTvSYmCVdH4HjgZ4Z94AAAAAvsHNwwAib/APSkICLAAZL0oOGK7VNYMPShBgQBCvSkIPShBQAAAgEgCgcBAv8IAf5/Ie1E0CDXScIBn9P/0wD0Bfhqf/hh+Gb4Yo4b9AVt+GpwAYBA9A7yvdcL//hicPhjcPhmf/hh4tMAAY4SgQIA1xgg+QFY+EIg+GX5EPKo3iP4RSBukjBw3vhCuvLgZSHTP9MfNCD4I7zyuSL5ACD4SoEBAPQOIJEx3vLQZvgACQA2IPhKI8jLP1mBAQD0Q/hqXwTTHwHwAfhHbvJ8AgEgEQsCAVgPDAEJuOiY/FANAdb4QW6OEu1E0NP/0wD0Bfhqf/hh+Gb4Yt7RcG1vAvhKgQEA9IaVAdcLP3+TcHBw4pEgjjJfM8gizwv/Ic8LPzExAW8iIaQDWYAg9ENvAjQi+EqBAQD0fJUB1ws/f5NwcHDiAjUzMehfAyHA/w4AmI4uI9DTAfpAMDHIz4cgzo0EAAAAAAAAAAAAAAAAD3RMfijPFiFvIgLLH/QAyXH7AN4wwP+OEvhCyMv/+EbPCwD4SgH0AMntVN5/+GcBCbkWq+fwEAC2+EFujjbtRNAg10nCAZ/T/9MA9AX4an/4Yfhm+GKOG/QFbfhqcAGAQPQO8r3XC//4YnD4Y3D4Zn/4YeLe+Ebyc3H4ZtH4APhCyMv/+EbPCwD4SgH0AMntVH/4ZwIBIBUSAQm7Fe+TWBMBtvhBbo4S7UTQ0//TAPQF+Gp/+GH4Zvhi3vpA1w1/ldTR0NN/39cMAJXU0dDSAN/RVHEgyM+FgMoAc89AzgH6AoBrz0DJc/sA+EqBAQD0hpUB1ws/f5NwcHDikSAUAISOKCH4I7ubIvhKgQEA9Fsw+GreIvhKgQEA9HyVAdcLP3+TcHBw4gI1MzHoXwb4QsjL//hGzwsA+EoB9ADJ7VR/+GcCASAYFgEJuORhh1AXAL74QW6OEu1E0NP/0wD0Bfhqf/hh+Gb4Yt7U0fhFIG6SMHDe+EK68uBl+AD4QsjL//hGzwsA+EoB9ADJ7VT4DyD7BCDQ7R7tU/ACMPhCyMv/+EbPCwD4SgH0AMntVH/4ZwIC2hsZAQFIGgAs+ELIy//4Rs8LAPhKAfQAye1U+A/yAAEBSBwAWHAi0NYCMdIAMNwhxwDcIdcNH/K8UxHdwQQighD////9vLHyfAHwAfhHbvJ8",
          1000
        )
      )
    }
    assertValue(result)(Result.Fee("330"))
  }

  it should "compress_zstd and decompress_zstd" in {
    val in =
      """Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."""
    val expected =
      """KLUv/QCAdQgAJhc5GJCnsA2AIm2tVzjno88mHb3Ttx9b8fXHHDAAMgAyAMUsVo6Pi3rPTDF2WDl510aHTwt44hrUxbn5oF6iUfiUiRbQhYo/PSM2WvKYt/hMIOQmuOaY/bmJQoRky46EF+cEd+Thsep5Hloo9DLCSwe1vFwcqIHycEKlMqBSo+szAiIBhkukH5kSIVlFukEWNF2SkIv6HBdPjFAjoUliCPjzKB/4jK91X95rTAKoASkPNqwUEw2Gkscdb3lR8YRYOR+P0sULCqzPQ8mQFJWnBSyP25mWIY2bFEUSJiGsWD+9NBqLhIAGDggQkLMbt5Y1aDR4uLKqwJXmQFPg/XTXIL7LCgspIF1YYplND4Uo"""
    val uncompressed = Base64.getEncoder.encodeToString(in.getBytes)
    val result = local { implicit ctx =>
      val compEf = call(Request.CompressZstd(uncompressed, Option(21)))
      val resEf = ef.flatMap {
        compEf
      } { compressed =>
        call(Request.DecompressZstd(compressed.compressed))
      }
      ef.flatMap(compEf)(e => ef.map(resEf)(f => (e.compressed, f.decompressed)))
    }
    assertValue(result)((expected, uncompressed))
  }
}
