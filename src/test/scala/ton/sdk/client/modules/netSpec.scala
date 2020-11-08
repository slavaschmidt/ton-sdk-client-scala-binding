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
  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val ef: Context.Effect[Future]         = futureEffect

  it should "subscribe_collection and get results" in {
    val filter   = Map("created_at" -> Map("gt" -> (System.currentTimeMillis() - 1000000))).asJson
    var counter  = 0
    val callback = (finished: Boolean, tpe: Long, in: Json) => counter += 1
    val resultF = devNet { implicit ctx =>
      for {
        handle <- call(Request.SubscribeCollection("messages", filter = Option(filter), result = "created_at"), callback)
        _ = assert(handle.handle > 0)
        _ = for (_ <- 0 to 500) Thread.sleep(10)
        un <- call(Request.Unsubscribe(handle.handle))
      } yield un
    }
    assertValue(resultF)(Json.Null)
    // TODO implement filter that actually receives something
    assert(counter == 0)
  }

  it should "subscribe_collection and get errors as JSON" in {
    val filter   = Map("now" -> Map("gt" -> System.currentTimeMillis())).asJson
    var counter  = 0
    val callback = (finished: Boolean, tpe: Long, in: Json) => counter += 1

    val resultF = devNet { implicit ctx =>
      for {
        handle <- call(Request.SubscribeCollection("messages", filter = Option(filter), result = "created_at"), callback)
        _ = assert(handle.handle > 0)
        _ = for (_ <- 0 to 500) Thread.sleep(10)
        un <- call(Request.Unsubscribe(handle.handle))
      } yield un
    }
    assertValue(resultF)(Json.Null)
    assert(counter > 0)
  }
}

class SyncNetSpec extends NetSpec[Try] {
  implicit override val ef: Context.Effect[Try] = tryEffect

  it should "not know subscribe_collection function" in {
    val callback = (end: Boolean, tpe: Long, s: Json) => println(s)
    val result = devNet { implicit ctx =>
      call(Request.SubscribeCollection("messages", None, result = "created_at"), callback)
    }
    assertSdkError(result)("Streaming synchronous requests aren't supported (function net.subscribe_collection)")
  }
}

abstract class NetSpec[T[_]] extends AsyncFlatSpec with SdkAssertions[T] {

  implicit val ef: Effect[T]

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
    val result = ef.unsafeGet(resultF)
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
    val result = ef.unsafeGet(resultF)
    assert(result.result.\\("now").forall(_.as[Long].toOption.get > 1562342740L))
  }

  it should "not wait_for_collection because of timeout" in {
    val result = devNet { implicit ctx =>
      call(Request.WaitForCollection("transactions", timeout = Option(1)))
    }
    assertSdkError(result)("WaitFor failed: Can not send http request: error sending request for url (https://net.ton.dev/graphql): operation timed out")
  }

  it should "unsubscribe handle that does not exist" in {
    val result = devNet { implicit ctx =>
      call(Request.Unsubscribe(100500))
    }
    assertValue(result)(Json.Null)
  }
}
