package ton.sdk.client.modules

import org.scalatest.{Assertion, Assertions}
import ton.sdk.client.jni.Binding
import ton.sdk.client.modules.Api.SdkClientError
import ton.sdk.client.modules.Context.Effect

trait SdkAssertions[T[_]] extends Assertions {
  Binding.loadNativeLibrary()

  implicit val fe: Effect[T]
  def assertValue[R, V](result: T[R])(v: V): Assertion                 = fe.unsafeGet(fe.map(result)(assertResult(v)))
  def assertExpression[R, V](result: T[R])(v: R => Boolean): Assertion = fe.unsafeGet(fe.map(result)(r => assert(v(r))))
  def assertSdkError[R](result: T[R])(message: String) = {
    val error = fe.recover(fe.map(result)(r => fail(s"Should not succeed but was $r"))) {
      case ex: SdkClientError => assert(ex.message === message)
    }
    fe.unsafeGet(error)
  }
  def base64(b: Array[Byte]) = new String(java.util.Base64.getEncoder.encode(b))
  def base64(s: String) = new String(java.util.Base64.getEncoder.encode(s.getBytes()))
}
