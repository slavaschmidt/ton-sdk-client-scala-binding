import io.circe.parser.decode
import ton.sdk.client.jni.{Binding, Handler}
import ton.sdk.client.modules.Api.SdkResultOrError._
import ton.sdk.client.modules.Api._
import ton.sdk.client.modules.{Client, Context, Processing}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

object Application extends App {
  Binding.loadNativeLibrary()

  def testLowLevel() = {
    val result = Binding.tcCreateContext("""{ "servers": ["net.ton.dev"] }""")
    println(result)
    // {"result":1}
    // {"result":{"build_info":{}}}
    // {"error":{"code":23,"message":"Invalid parameters: EOF while parsing a string at line 1 column 5\nparams: { \" }","data":{"core_version":"1.0.0"}}}
    val id = result.filter(_.isDigit).toLong
    // println(Binding.tcRequestSync(id, "client.build_info", ""))
    val callback: Handler = (rId: Long, paramsJson: String, responseType: Long, finished: Boolean) => {
      println(s"$rId [$responseType]($finished): $paramsJson")
    }

    Binding.request(1, "client.get_api_r eference", "", callback)
    // Binding.request(1, "client.version", "", callback)
    // Binding.request(1, "client.v ersion", "", callback)

    // println(Binding.tcRequestSync(1, "client.get_api_reference", ""))

    //Binding.request(1, "client.version", "", callback)
    //  Binding.request(1, "client.get_api_reference", "", callback)
    //  Binding.request(1, "client.get_api_reference", "", callback)
    //  Binding.request(1, "client.get_api_reference", "", callback)
    //  Binding.request(1, "client.get_api_reference", "", callback)
    //  Binding.request(1, "client.get_api_reference", "", callback)

    Binding.tcDestroyContext(id)
  }
  import scala.concurrent.ExecutionContext.Implicits.global
  import Context._

//  def testBasicContext = {
//    Context.synchronous(ClientConfig.local) { implicit ctx =>
//      println(requestSync("client.build_info", ""))
//      println(requestSync("client.version", ""))
//    }
//  }
//
//  def testBasicAsyncContext = {
//    devNet { implicit ctx =>
//      requestAsync("client.setup", "").onComplete(println)
//      val f = requestAsync("client.build_info", "")
//      f.onComplete(println)
//      f
//    }
//  }

  // testBasicContext.map(Await.result(_, 10.seconds))

  import io.circe.generic.auto._

  def testEncoders(): Unit = {
    println(fromJsonWrapped[Long]("""{"result":1}"""))
    println(decode[SdkError[Long]]("""{"error":{"code":25,"message":"Unknown function: version","data":{"core_version":"1.0.0"}}}"""))
    println(fromJsonWrapped[Long]("""{"error":{"code":25,"message":"Unknown function: version","data":{"core_version":"1.0.0"}}}"""))
  }
//  testEncoders()

  def testClientAsync() = {
    implicit val fe = futureEffect
    local { implicit ctx =>
      import Client._
      val f = call(Client.Request.Version)
      f.onComplete(println)
      val g = call(Client.Request.BuildInfo)
      g.onComplete(println)
      val h = call(Client.Request.ApiReference)
      h.onComplete { c =>
        println("H:" + c)
      }
      val i = call(Client.Request.ApiReference)
      i.onComplete { c =>
        println("I:" + c)
      }
      val j = call(Client.Request.ApiReference)
      j.onComplete { c =>
        println("j:" + c)
      }
      Future.sequence(Seq(f, g, h, i, j))
    //      println(request("client.version", ""))
    }
//    devNet { implicit ctx =>
//      requestAsync("client.setup", "").onComplete(println)
//      val f = requestAsync("client.build_info", "")
//      f.onComplete(println)
//      f
//    }

  }

  def testProcessingAsync() = {
    implicit val fe = futureEffect
    local { implicit ctx =>
      import Processing._
      call(Processing.Request.sendMessage("EEFFFEEC", None))
    }
  }

  def testUtilAsync() = {
    implicit val fe = futureEffect
    local { implicit ctx =>
      import ton.sdk.client.modules.Utils._
      call(Request.ConvertAddress("this is my address", AddressOutputFormat.accountId))
    }
  }

  def testUtilSync() = {
    implicit val fe = tryEffect
    local { implicit ctx =>
      import ton.sdk.client.modules.Utils._
      call(Request.ConvertAddress("this is my address", AddressOutputFormat.accountId))
    }
  }
  val result = Await.result(testUtilAsync(), 10.seconds)
  println(result)

  // Thread.sleep(10000)
  // testClientAsync.map(Await.ready(_, 10.seconds))

}
