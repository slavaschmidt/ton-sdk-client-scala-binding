package ton.sdk.client.modules

import org.scalatest.flatspec._
import org.scalatest.matchers._
import ton.sdk.client.modules.Context._
import ton.sdk.client.modules.Client._

abstract class ClientSpec[T[_]]  extends AsyncFlatSpec with should.Matchers {

  implicit val fe: Effect[T]

  behavior of "Client"

  it should "get expected version" in {
    val result = local { implicit ctx =>
      call(Request.Version)
    }
    fe.unsafeGet(fe.map(result)(assertResult(Result.Version("1.0.0"))))
  }

  it should "get response of type BuildInfo" in {
    val result: T[Result.BuildInfo] = local { implicit ctx =>
      call(Request.BuildInfo)
    }
    fe.unsafeGet(fe.map(result)(r => assert(r.build_info.isObject)))
  }

  it should "get expected version from the api description" in {
    val result = local { implicit ctx =>
      call(Request.ApiReference)
    }
    val api = fe.map(result)(_.api)
    fe.unsafeGet(fe.map(api)(api => assert(api.version == "1.0.0" && api.modules.length == 8 &&
      api.modules.map(_.name).sorted == List("client", "utils", "crypto", "boc", "abi", "processing", "tvm", "net").sorted)))
  }
}
