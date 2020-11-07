package ton.sdk.client.modules

import io.circe.Json
import io.circe.syntax._
import org.scalatest.flatspec.AsyncFlatSpec
import ton.sdk.client.binding.{ClientConfig, OrderBy}
import ton.sdk.client.modules.Context._
import ton.sdk.client.modules.Net.Request

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class AsyncNetSpec extends NetSpec[Future] {
  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override implicit val fe: Context.Effect[Future] = futureEffect
}
class SyncNetSpec extends NetSpec[Try] {
  implicit override val fe: Context.Effect[Try] = tryEffect
}
abstract class NetSpec[T[_]] extends AsyncFlatSpec with SdkAssertions[T] {

  implicit val fe: Effect[T]

  behavior of "Net"

  it should "query_collection blocks_signatures" in {
    val result = devNet { implicit ctx =>
      call(Request.QueryCollection("blocks_signatures", result = "id", limit = Option(1)))
    }
    assertExpression(result)(_.result.nonEmpty)
  }

  it should "query_collection accounts" in {
    val result = devNet { implicit ctx =>
      call(Request.QueryCollection("accounts", result = "id balance", limit = Option(5)))
    }
    assertExpression(result)(_.result.size == 5)
  }

  it should "query_collection messages" in {
    val filter = Map("created_at" -> Map("gt" -> 1562342740)).asJson
    val resultF = devNet { implicit ctx =>
      call(Request.QueryCollection("messages", filter = Option(filter), result = "body created_at", order = Option(Seq(OrderBy("created_at", "DESC"))), limit = Option(10)))
    }
    val result = fe.unsafeGet(resultF)
    val bodies = result.result.flatMap(_ \\ "body")
    val times  = result.result.flatMap((_ \\ "created_at")).map(_.as[Long].toOption.get)
    assert(bodies.count(_ != Json.Null) <= 10)
    assert(times.forall(_ > 1562342740L))
  }

  it should "not query_collection without network" in {
    implicit val ctx = Context.create(ClientConfig()).get
    val result       = call(Request.QueryCollection(collection = "messages"))
    assertSdkError(result)("SDK is initialized without network config")
  }

  it should "not query_collection" in {
    val result = devNet { implicit ctx =>
      call(Request.QueryCollection(collection = "messages"))
    }
    assertSdkError(result)("Query failed: Graphql server returned error: Syntax Error: Expected Name, found \"}\".")
  }

  it should "wait_for_collection transactions" in {
    val filter = Map("now" -> Map("gt" -> 1562342740L)).asJson
    val resultF = devNet { implicit ctx =>
      call(Request.WaitForCollection("transactions", filter = Option(filter), result = "id now"))
    }
    val result = fe.unsafeGet(resultF)
    assert(result.result.\\("now").forall(_.as[Long].toOption.get > 1562342740L))
  }

  it should "not wait_for_collection because of timeout" in {
    val result = devNet { implicit ctx =>
      call(Request.WaitForCollection("transactions", timeout = Option(1)))
    }
    assertSdkError(result)("WaitFor failed: Can not send http request: error sending request for url (https://net.ton.dev/graphql): operation timed out")
  }

  // TODO implementation is missing yet
  it should "subscribe_collection" in {
    val filter = Map("now" -> Map("gt" -> System.currentTimeMillis())).asJson
    val resultF = devNet { implicit ctx =>
      call(Request.SubscribeCollection("messages", filter=Option(filter), result = "created_at"))
    }
    val result = fe.unsafeGet(resultF)
    val messages = result.elements.take(10)

    assert(messages.size == 10) // TODO check something else

    val un = devNet { implicit ctx =>
      call(Request.Unsubscribe(100500))
    }
    assertValue(un)(Json.Null)

  }

  it should "unsubscribe handle that does not exist" in {
    val result = devNet { implicit ctx =>
      call(Request.Unsubscribe(100500))
    }
    assertValue(result)(Json.Null)
  }
}
