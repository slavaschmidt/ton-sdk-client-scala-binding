package ton.sdk.client.modules

import io.circe.Json
import io.circe.literal.JsonStringContext
import io.circe.syntax._
import org.scalatest.Succeeded
import org.scalatest.flatspec.AsyncFlatSpec
import ton.sdk.client.binding.{ClientConfig, Context, OrderBy}
import ton.sdk.client.binding.Context._
import ton.sdk.client.modules.Net.{FieldAggregation, Request, Result}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.Try

class AsyncNetSpec extends NetSpec[Future] {
  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val ef: Context.Effect[Future]         = futureEffect

  it should "subscribe_collection and get results" in {
    val now = 1562342740L

    val filter = json"""{"now":{"gt":$now}}"""

    val messages = devNet { implicit ctx =>
      for {
        (handle, messages, _) <- callS(Request.SubscribeCollection("transactions", filter = Option(filter), result = "id now"))
        _ = assert(handle.handle > 0)
        m = messages.collect(25.seconds)
        _ = assert(handle.handle > 0)
        _ <- call(Request.Unsubscribe(handle.handle))
      } yield m
    }
    assertExpression(messages)(_.nonEmpty)
  }

  it should "subscribe_collection and get errors as JSON" in {
    val filter = json"""{"now":{"gt":100000000}}"""
    val errors = devNet { implicit ctx =>
      for {
        (handle, _, errors) <- callS(Request.SubscribeCollection("transactions", filter = Option(filter), result = "created_at"))
        _ = assert(handle.handle > 0)
        e = errors.collect(5.seconds)
        _ <- call(Request.Unsubscribe(handle.handle))
      } yield e
    }
    assertExpression(errors)(_.nonEmpty)
  }

  it should "suspend resume" in {
    val filter = Map("created_at" -> Map("gt" -> (System.currentTimeMillis / 1000))).asJson

    val result = devNet { implicit ctx =>
      for {
        (handle, messages, _) <- callS(Request.SubscribeCollection("messages", filter = Option(filter), result = "body created_at"))
        _ = assert(handle.handle > 0)
        m = messages.collect(25.seconds)
        _ = assert(m.nonEmpty)
        _ <- call(Request.Suspend)
        n = messages.collect(25.seconds)
        _ = assert(n.size == m.size)
        _ <- call(Request.Resume)
        p = messages.collect(25.seconds)
        r = assert(p.size > m.size)
        _ <- call(Request.Unsubscribe(handle.handle))
      } yield r
    }
    assertValue(result)(Succeeded)
  }

}

class SyncNetSpec extends NetSpec[Try] {
  implicit override val ef: Context.Effect[Try] = tryEffect

  // No need for this test as this won't compile

  //  it should "not know subscribe_collection function" in {
//    val result = devNet { implicit ctx =>
//      callS(Request.SubscribeCollection("messages", None, result = "created_at"))
//    }
//    assertSdkError(result)("Streaming synchronous requests aren't supported (function net.subscribe_collection)")
//  }
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
    val query: T[Result.QueryCollection] = devNet { implicit ctx =>
      call(Request.QueryCollection("messages", filter = Option(filter), result = "body created_at", order = Option(Seq(OrderBy("created_at", "DESC"))), limit = Option(10)))
    }
    val result = ef.unsafeGet(query)
    val bodies = result.result.flatMap(_ \\ "body")
    val times  = result.result.flatMap((_ \\ "created_at")).map(_.as[Long].toOption.get)
    assert(bodies.count(_ != Json.Null) <= 10)
    assert(times.forall(_ > 1562342740L))
  }

  it should "not query_collection without network" in {
    implicit val ctx = Context.create(ClientConfig()).get
    val result       = call(Request.QueryCollection(collection = "messages", result = ""))
    val assertion    = assertSdkError(result)("SDK is initialized without network config")
    // do not forget to close the context or see a warning at runtime
    // "Context(4) was not closed as expected, this is a programming error"
    ctx.close()
    assertion
  }

  it should "not query_collection" in {
    val result = devNet { implicit ctx =>
      call(Request.QueryCollection(collection = "messages", result = ""))
    }
    assertSdkError(result)("Query failed: Graphql server returned error: Field \"messages\" of type \"[Message]\" must have a selection of subfields. Did you mean \"messages { ... }\"?")
  }

  it should "batch_query" in {
    val filter = Map("now" -> Map("gt" -> 20)).asJson
    val q1: Request.QueryCollection = Request.QueryCollection("blocks_signatures", filter = None, result = "id", order = None, limit = Option(1))
    val q2 = Request.AggregateCollection("accounts", filter = None, fields = Some(Seq(FieldAggregation("", "COUNT"))))
    val q3 = Request.WaitForCollection("transactions", filter = Some(filter), result = "id now", timeout = None)

    val result = devNet { implicit ctx =>
      call(Request.BatchQuery(Seq(q1,q2,q3)))
    }
    assertExpression(result)(_.results.size == 3)
  }

  it should "wait_for_collection transactions" in {
    val filter = Map("now" -> Map("gt" -> 1562342740L)).asJson
    val resultF = devNet { implicit ctx =>
      call(Request.WaitForCollection("transactions", "id now", Option(filter)))
    }
    val result = ef.unsafeGet(resultF)
    assert(result.result.\\("now").forall(_.as[Long].toOption.get > 1562342740L))
  }

  it should "not wait_for_collection because of timeout" in {
    val result = devNet { implicit ctx =>
      call(Request.WaitForCollection("transactions", "", timeout = Option(1)))
    }
    assertSdkError(result)("WaitFor failed: Graphql server returned error: Field \"transactions\" of type \"[Transaction]\" must have a selection of subfields. Did you mean \"transactions { ... }\"?")
  }

  it should "unsubscribe handle that does not exist" in {
    val result = devNet { implicit ctx =>
      call(Request.Unsubscribe(100500))
    }
    assertValue(result)(Json.Null)
  }

  it should "query" in {
    val variables = Option(Map("time" -> (System.currentTimeMillis / 1000)).asJson)
    val query     = "query($time: Float){messages(filter:{created_at:{ge:$time}}limit:5){id}}"
    val result = devNet { implicit ctx =>
      call(Request.Query(query, variables))
    }
    assertValue(result)(Result.Query(json"""{"data":{"messages":[]}}"""))
  }

  it should "aggregate_collection" in {
    val result = devNet { implicit ctx =>
      call(Request.AggregateCollection("accounts", None, Some(Seq(FieldAggregation("", "COUNT")))))
    }
    assertExpression(result){r:Result.CollectionAggregation => r.values.head.asString.exists(_.toInt > 0)}
  }

  it should "not query" in {
    val variables = Option(Map("time" -> (System.currentTimeMillis / 1000 - 60)).asJson)
    val query     = "query($time: Float){(filter:{created_at:{ge:$time}}limit:5){id}}"
    val result = devNet { implicit ctx =>
      call(Request.Query(query, variables))
    }
    assertSdkError(result)("Query failed: Graphql server returned error: Syntax Error: Expected Name, found \"(\".")
  }

  it should "find_last_shard_block" in {
    val result = devNet { implicit ctx =>
      call(Request.FindLastShardBlock(giverAddress))
    }
    assertExpression(result)(_.block_id.length == 64)
  }

  it should "not find_last_shard_block" in {
    val result = devNet { implicit ctx =>
      call(Request.FindLastShardBlock("baker street, london 221b"))
    }
    assertSdkError(result)("Invalid address [fatal error]: baker street, london 221b")
  }

  // commented out in sdk tests
  ignore should "fetch_endpoints" in {
    val network = ClientConfig.DEV_NET.network.map(cfg => cfg.copy(endpoints = Seq("cinet.tonlabs.io", "cinet2.tonlabs.io")))
    val config  = ClientConfig.DEV_NET.copy(network = network)
    val result = ef.managed(config) { implicit ctx =>
      call(Request.FetchEndpoints)
    }
    assertExpression(result) { r =>
      println(r); r.endpoints.nonEmpty
    }
  }

  it should "set_endpoints" in {
    val result = devNet { implicit ctx =>
      call(Request.SetEndpoints(Seq("localhost")))
    }
    assertExpression(result)(_ == (()))
  }

}
