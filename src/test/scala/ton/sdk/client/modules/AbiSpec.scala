package ton.sdk.client.modules

import io.circe.parser.decode
import io.circe.generic.auto._
import org.scalatest.flatspec._
import ton.sdk.client.binding.KeyPair
import ton.sdk.client.modules.Abi._
import ton.sdk.client.modules.Context._

import scala.io.Source
import scala.util.Try

class SyncAbiSpec extends AbiSpec[Try] {
  implicit override val fe: Context.Effect[Try] = tryEffect
}

abstract class AbiSpec[T[_]] extends AsyncFlatSpec with SdkAssertions[T] {

  behavior of "Abi"

  implicit val fe: Effect[T]

  val keyPair = KeyPair(public = "4c7c408ff1ddebb8d6405ee979c716a14fdd6cc08124107a61d3c25597099499", secret = "cc8929d635719612a9478b9cd17675a39cfad52d8959e8a177389b8c0b9122a7")
  val abiSrc = Source.fromResource("Events.abi.json").mkString
  val abi = decode[Abi](abiSrc).toOption.get
//  val tvcSrc = Source.fromResource("Events.tvc").mkString
//  println(tvcSrc.length)
//  val tcv = base64(tvcSrc)
  println(abi)
  val events_time = 1599458364291L
  val events_expire = 1599458404

  it should "decode_message Input" in {
    val message = "te6ccgEBAwEAvAABRYgAC31qq9KF9Oifst6LU9U6FQSQQRlCSEMo+A3LN5MvphIMAQHhrd/b+MJ5Za+AygBc5qS/dVIPnqxCsM9PvqfVxutK+lnQEKzQoRTLYO6+jfM8TF4841bdNjLQwIDWL4UVFdxIhdMfECP8d3ruNZAXul5xxahT91swIEkEHph08JVlwmUmQAAAXRnJcuDX1XMZBW+LBKACAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=="
    val result = local { implicit ctx =>
      call(Request.DecodeMessage(abi, message))
    }
    fe.unsafeGet(fe.map(result)(assertResult(Result.DecodedMessage(""))))
  }

  it should "decode_message Event" in {
    val message = "te6ccgEBAQEAVQAApeACvg5/pmQpY4m61HmJ0ne+zjHJu3MNG8rJxUDLbHKBu/AAAAAAAAAMJL6z6ro48sYvAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABA"
    val result = local { implicit ctx =>
      call(Request.DecodeMessage(abi, message))
    }
    fe.unsafeGet(fe.map(result)(assertResult(Result.DecodedMessage(""))))
  }

  it should "decode_message Output" in {
    val message = "te6ccgEBAQEAVQAApeACvg5/pmQpY4m61HmJ0ne+zjHJu3MNG8rJxUDLbHKBu/AAAAAAAAAMKr6z6rxK3xYJAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABA"
    val result = local { implicit ctx =>
      call(Request.DecodeMessage(abi, message))
    }
    fe.unsafeGet(fe.map(result)(assertResult(Result.DecodedMessage(""))))
  }

  it should "not decode_message" in {
    val result = local { implicit ctx =>
      call(Request.DecodeMessage(abi, "Oh Weh"))
    }
    assertSdkError(result)("")
  }

  // TODO make for comprehension
  it should "decode_message_body" in {
    val message = "te6ccgEBAwEAvAABRYgAC31qq9KF9Oifst6LU9U6FQSQQRlCSEMo+A3LN5MvphIMAQHhrd/b+MJ5Za+AygBc5qS/dVIPnqxCsM9PvqfVxutK+lnQEKzQoRTLYO6+jfM8TF4841bdNjLQwIDWL4UVFdxIhdMfECP8d3ruNZAXul5xxahT91swIEkEHph08JVlwmUmQAAAXRnJcuDX1XMZBW+LBKACAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=="
    val result = local { implicit ctx =>
      val parsed = fe.unsafeGet(call(Boc.Request.ParseMessage(message)))
      call(Request.DecodeMessage(abi, parsed.parsed.body))
    }
    fe.unsafeGet(fe.map(result)(assertResult(Result.DecodedMessage(""))))
  }


}
