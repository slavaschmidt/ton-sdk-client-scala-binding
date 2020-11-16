package ton.sdk.client.modules

import io.circe._
import io.circe.syntax._
import org.scalatest.{Assertion, Assertions}
import ton.sdk.client.binding.{CallSet, Signer}
import ton.sdk.client.jni.{Binding, NativeLoader}
import ton.sdk.client.modules.Abi.AbiJson
import ton.sdk.client.binding.Api.SdkClientError
import ton.sdk.client.binding.Context._
import ton.sdk.client.modules.Processing.{MessageEncodeParams, Result}

import scala.language.higherKinds

trait SdkAssertions[T[_]] extends Assertions {
  NativeLoader.apply()

  implicit val ef: Effect[T]
  def assertValue[R, V](result: T[R])(v: V): Assertion                 = ef.unsafeGet(ef.map(result)(assertResult(v)))
  def assertExpression[R, V](result: T[R])(v: R => Boolean): Assertion = ef.unsafeGet(ef.map(result)(r => assert(v(r))))
  def assertSdkError[R](result: T[R])(message: String) = {
    val error = ef.recover(ef.map(result)(r => fail(s"Should not succeed but was $r"))) {
      case ex: SdkClientError => assert(ex.message === message)
    }
    ef.unsafeGet(error)
  }
  def base64(b: Array[Byte]) = new String(java.util.Base64.getEncoder.encode(b))
  def base64(s: String)      = new String(java.util.Base64.getEncoder.encode(s.getBytes()))

  def unsafeDecode[J: Decoder](j: Json): J = j.as[J] match {
    case Left(value)  => fail("Could not decode json to expected type", value)
    case Right(value) => value
  }

  def sendGrams(address: String): T[Result.ResultOfProcessMessage] = {
    val giver   = "0:653b9a6452c7a982c6dc92b2da9eba832ade1c467699ebb3b43dca6d77b780dd"
    val abi     = AbiJson.fromResource("Giver.abi.json", getClass.getClassLoader).toOption.get
    val callSet = CallSet("grant", input = Option(Map("addr" -> address.asJson)))
    val params  = MessageEncodeParams(abi, Signer.none, Option(giver), None, Option(callSet))
    devNet { implicit ctx =>
      call(Processing.Request.ProcessMessageWithoutEvents(params))
    }
  }

}
