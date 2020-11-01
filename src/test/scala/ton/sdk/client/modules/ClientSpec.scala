package ton.sdk.client.modules

import io.circe.{Json, JsonObject}
import org.scalatest.flatspec._
import org.scalatest.matchers._
import ton.sdk.client.modules.Context._
import ton.sdk.client.modules.Client._

import scala.concurrent.Future

class ClientSpec extends AsyncFlatSpec with should.Matchers {

  behavior of "Utils"

  it should "get expected version" in {
    local { implicit ctx =>
      request(Request.Version)
    }.map(assertResult(Result.Version("1.0.0")))
  }

  it should "get response of type BuildInfo" in {
    local { implicit ctx =>
      request(Request.BuildInfo)
    }.map(r => assert(r.build_info.isObject))
  }

  it should "get expected version from the api description" in {
    local { implicit ctx =>
      request(Request.ApiReference)
    }.map(_.api).map(api => assert(api.version == "1.0.0" && api.modules.length == 8))
  }

  it should "be able to do stuff in parallel in single context and in multiple contexts" in {
    val r1 = local { implicit ctx =>
      Future.sequence(for (_ <- 1 to 10) yield request(Request.ApiReference))
    }
    val r2 = Future
      .sequence(for (_ <- 1 to 10) yield local { implicit ctx =>
        request(Request.ApiReference)
      })
    for {
      result1 <- r1
      result2 <- r2
    } yield assert(result1 == result2 && result1.size == 10)
  }

}
