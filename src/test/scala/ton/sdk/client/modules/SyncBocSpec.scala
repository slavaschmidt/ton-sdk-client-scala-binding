package ton.sdk.client.modules

import org.scalatest.Assertion
import ton.sdk.client.modules.Api.SdkClientError
import ton.sdk.client.modules.Boc.Request
import ton.sdk.client.modules.Context.{call, local, tryEffect}

import scala.language.implicitConversions
import scala.util.Try

class SyncBocSpec extends BocSpec[Try] {
  override implicit val fe: Context.Effect[Try] = tryEffect

  implicit def tryToAssertion(t: Try[Assertion]) = Context.fromTry(t)

  it should "fail to parse message" in {
    val message = "I'm not your message"
    val result = local { implicit ctx =>
      call(Request.ParseMessage(message))
    }
    fe.map(result)(_ => fail("Should not succeed")).recover {
      case ex: SdkClientError => assert(ex.message == "Invalid BOC: error decode message BOC base64: Invalid byte 39, offset 1.")
    }
  }

  it should "fail to parse transaction" in {
    val transaction = "I'm not your transaction"
    val result = local { implicit ctx =>
      call(Request.ParseTransaction(transaction))
    }
    fe.map(result)(_ => fail("Should not succeed")).recover {
      case ex: SdkClientError => assert(ex.message == "Invalid BOC: error decode transaction BOC base64: Invalid byte 39, offset 1.")
    }
  }

  it should "fail to parse account" in {
    val account = "I'm not your account=="
    val result = local { implicit ctx =>
      call(Request.ParseAccount(account))
    }
    fe.map(result)(_ => fail("Should not succeed")).recover {
      case ex: SdkClientError => assert(ex.message == "Invalid BOC: error decode account BOC base64: Invalid byte 39, offset 1.")
    }
  }

  it should "fail to parse block" in {
    val block = "I'm not your block=="
    val result = local { implicit ctx =>
      call(Request.ParseBlock(block))
    }
    fe.map(result)(_ => fail("Should not succeed")).recover {
      case ex: SdkClientError => assert(ex.message == "Invalid BOC: error decode block BOC base64: Invalid byte 39, offset 1.")
    }
  }
}
