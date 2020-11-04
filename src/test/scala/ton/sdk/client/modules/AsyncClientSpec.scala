package ton.sdk.client.modules

import ton.sdk.client.modules.Client.Request
import ton.sdk.client.modules.Context.{call, futureEffect, local}

import scala.concurrent.{ExecutionContext, Future}

class AsyncClientSpec extends ClientSpec[Future] {
  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override implicit val fe: Context.Effect[Future] = futureEffect

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
