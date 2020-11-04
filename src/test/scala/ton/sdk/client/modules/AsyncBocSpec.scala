package ton.sdk.client.modules

import ton.sdk.client.modules.Api.SdkClientError
import ton.sdk.client.modules.Boc.Request
import ton.sdk.client.modules.Context.{call, futureEffect, local}

import scala.concurrent.{ExecutionContext, Future}

class AsyncBocSpec extends BocSpec[Future] {
  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override implicit val fe: Context.Effect[Future] = futureEffect

  it should "fail to parse message" in {
    val message = "I'm not your message"
    val result = local { implicit ctx =>
      call(Request.ParseMessage(message))
    }
    result.map(_ => fail("Should not succeed")).recover {
      case ex: SdkClientError => assert(ex.message == "Invalid BOC: error decode message BOC base64: Invalid byte 39, offset 1.")
    }
  }

  it should "fail to parse transaction" in {
    val transaction = "I'm not your transaction"
    val result = local { implicit ctx =>
      call(Request.ParseTransaction(transaction))
    }
    result.map(_ => fail("Should not succeed")).recover {
      case ex: SdkClientError => assert(ex.message == "Invalid BOC: error decode transaction BOC base64: Invalid byte 39, offset 1.")
    }
  }

  it should "fail to parse account" in {
    val account = "I'm not your account=="
    val result = local { implicit ctx =>
      call(Request.ParseAccount(account))
    }
    result.map(_ => fail("Should not succeed")).recover {
      case ex: SdkClientError => assert(ex.message == "Invalid BOC: error decode account BOC base64: Invalid byte 39, offset 1.")
    }
  }

  it should "fail to parse block" in {
    val block = "I'm not your block=="
    val result = local { implicit ctx =>
      call(Request.ParseBlock(block))
    }
    result.map(_ => fail("Should not succeed")).recover {
      case ex: SdkClientError => assert(ex.message == "Invalid BOC: error decode block BOC base64: Invalid byte 39, offset 1.")
    }
  }
}
