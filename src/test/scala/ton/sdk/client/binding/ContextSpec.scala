package ton.sdk.client.binding

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers._
import ton.sdk.client.binding.Context._
import ton.sdk.client.jni.Binding
import ton.sdk.client.modules.Client

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

class ContextSpec extends AsyncFlatSpec {

  "Context" should "throw an exception if library is not loaded" in {
    try {
      implicit val effect: Context.Effect[Try] = Context.tryEffect
      Context(1).request(Client.Request.BuildInfo)
      fail("Should throw an exception")
    } catch {
      case _: UnsatisfiedLinkError => succeed
    }
  }

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

    val c       = Context.create(ClientConfig.TEST_NET).get
    val result1 = c.request(Client.Request.BuildInfo)
    Await.ready(result1, 10.seconds)
    c.close()
    val result2 = c.request(Client.Request.BuildInfo)
    Await.ready(result2, 10.seconds)
    result1.value.get.isSuccess shouldEqual true
    result2.value.get.isSuccess shouldEqual false
  }
}
