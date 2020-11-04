package ton.sdk.client.modules

import org.scalatest.{Assertion, Assertions}
import ton.sdk.client.modules.Api.SdkClientError
import ton.sdk.client.modules.Context.Effect

trait SdkAssertions[T[_]] extends Assertions {
  implicit val fe: Effect[T]
  def assertValue[R, V](result: T[R])(v: V): Assertion                 = fe.unsafeGet(fe.map(result)(assertResult(v)))
  def assertExpression[R, V](result: T[R])(v: R => Boolean): Assertion = fe.unsafeGet(fe.map(result)(r => assert(v(r))))
  def assertSdkError[R](result: T[R])(message: String) = fe.recover(fe.map(result)(r => fail(s"Should not succeed but was $r"))) {
      case ex: SdkClientError => assert(ex.message === message)
    }
}
