package ton.sdk.client.binding

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers._
import ton.sdk.client.binding.Context._
import ton.sdk.client.jni.NativeLoader
import ton.sdk.client.modules.Client
import ton.sdk.client.modules.Net.Request.SubscribeCollection

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Try}

class ContextSpec extends AsyncFlatSpec {

  "Sync Context" should "not allow usage if not open" in {
    implicit val effect: Context.Effect[Try] = Context.tryEffect
    NativeLoader.apply()
    val c       = Context.create(ClientConfig.MAIN_NET).get
    val result1 = c.request(Client.Request.BuildInfo)
    c.close()
    val result2 = c.request(Client.Request.BuildInfo)
    result1.isSuccess shouldEqual true
    result2.isSuccess shouldEqual false
  }

  "Async Context" should "not allow usage if not open" in {
    implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    implicit val ef: Effect[Future]                 = futureEffect

    val r = testNet { implicit ctx =>
      ctx.close()
      call(Client.Request.BuildInfo)
    }
    Await.ready(r, 1.second)
    r.value.get.isFailure shouldBe true
  }

  "Streaming async Context" should "not allow usage if closed" in {
    implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    implicit val ef: Effect[Future]                 = futureEffect

    val r = mainNet { implicit ctx =>
      ctx.close()
      callS(SubscribeCollection("this", "doesn't matter"))
    }
    Await.ready(r, 1.second)
    r.value.get.isFailure shouldBe true
  }

  "Failed try" should "convert to failed future" in {
    val future = fromTry(Failure(new Exception("Houston, we have a problem")))
    future.value.get.isSuccess shouldBe false
  }
}
