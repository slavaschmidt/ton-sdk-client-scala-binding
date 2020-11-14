package ton.sdk.client.binding

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers._
import ton.sdk.client.binding.Context._
import ton.sdk.client.jni.Binding
import ton.sdk.client.modules.Client
import ton.sdk.client.modules.Client.Result

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Try}

class ContextSpec extends AsyncFlatSpec {

  "Sync Context" should "not allow usage if not open" in {
    implicit val effect: Context.Effect[Try] = Context.tryEffect
    Binding.loadNativeLibrary()
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

    var result1: Future[Result.BuildInfo] = null
    var result2: Future[Result.BuildInfo] = null

    val r1r2 = testNet { implicit ctx =>
      result1 = call(Client.Request.BuildInfo)
      Await.ready(result1, 10.seconds)
      ctx.close()
      result2 = call(Client.Request.BuildInfo)
      Future.sequence(Seq(result1, result2))
    }
    Await.ready(r1r2, 10.seconds)
    r1r2.value.get.isFailure shouldBe true
  }

  "Streaming async Context" should "not allow usage if closed" in {
    implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
    implicit val ef: Effect[Future]                 = futureEffect

    var result1: Future[Result.BuildInfo] = null
    var result2: Future[Result.BuildInfo] = null

    val r1r2 = mainNet { implicit ctx =>
      result1 = call(Client.Request.BuildInfo)
      Await.ready(result1, 10.seconds)
      ctx.close()
      result2 = call(Client.Request.BuildInfo)
      Await.ready(result2, 10.seconds)
      Future.sequence(Seq(result1, result2))
    }
    Await.ready(r1r2, 10.seconds)
    r1r2.value.get.isFailure shouldBe true
  }

  "Failed try" should "convert to failed future" in {
    val future = fromTry(Failure(new Exception("Houston, we have a problem")))
    future.value.get.isSuccess shouldBe false
  }
}
