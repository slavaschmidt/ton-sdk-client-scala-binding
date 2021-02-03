package ton.sdk.client.modules

import org.scalatest.flatspec._
import ton.sdk.client.binding.Context
import ton.sdk.client.binding.Context._
import ton.sdk.client.modules.Client.Result.BuildInfo
import ton.sdk.client.modules.Client._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.{Success, Try}

class AsyncClientSpec extends ClientSpec[Future] {
  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val ef: Context.Effect[Future]         = futureEffect

  it should "be able to do stuff in parallel in single context and in multiple contexts" in {
    val r1 = local { implicit ctx =>
      Future.sequence(for (_ <- 1 to 10) yield call(Request.ApiReference))
    }
    val r2 = Future
      .sequence(for (_ <- 1 to 10) yield local { implicit ctx =>
        call(Request.ApiReference)
      })
    for {
      result1 <- r1
      result2 <- r2
    } yield assert(result1 == result2 && result1.size == 10)
  }

}

class SyncClientSpec extends ClientSpec[Try] {
  implicit override val ef: Context.Effect[Try] = tryEffect

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
  }
}

abstract class ClientSpec[T[_]] extends AsyncFlatSpec with SdkAssertions[T] {

  implicit val ef: Effect[T]

  behavior of "Client"

  it should "get expected version" in {
    val result = local { implicit ctx =>
      call(Request.Version)
    }
      assertValue(result)(Result.Version("1.6.1"))
  }

  it should "get response of type BuildInfo" in {
    val result: T[BuildInfo] = local { implicit ctx =>
      call(Request.BuildInfo)
    }
    assertExpression(result) { r =>
      r.build_number > -1
    }
  }

  it should "get expected version from the api description" in {
    val result = local { implicit ctx =>
      call(Request.ApiReference)
    }
    val api = ef.map(result)(_.api)
    assertExpression(api) { r =>
      r.modules.length == 9 &&
      r.modules.map(_.name).sorted == List("abi", "boc", "client", "crypto", "debot", "net", "processing", "tvm", "utils")
    }
  }
}
