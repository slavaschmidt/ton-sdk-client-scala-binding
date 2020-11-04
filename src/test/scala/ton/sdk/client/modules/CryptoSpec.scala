package ton.sdk.client.modules

import org.scalatest.flatspec._
import org.scalatest.matchers._
import ton.sdk.client.modules.Api.SdkClientError
import ton.sdk.client.modules.Crypto._
import ton.sdk.client.modules.Context._

import scala.concurrent.{ExecutionContext, Future}

/*abstract*/
class CryptoSpec[T[_]] extends AsyncFlatSpec with SdkAssertions[Future] {

  // implicit val fe: Effect[T]

  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit /*override*/ val fe: Context.Effect[Future]     = futureEffect

  behavior of "Crypto"

  it should "convert_public_key_to_ton_safe_format" in {
    val result = local { implicit ctx =>
      call(Request.PublicKey("06117f59ade83e097e0fb33e5d29e8735bda82b3bf78a015542aaa853bb69600"))
    }
    assertValue(result)(Result.TonPublicKey("PuYGEX9Zreg-CX4Psz5dKehzW9qCs794oBVUKqqFO7aWAOTD"))
  }

  it should "not convert_public_key_to_ton_safe_format" in {
    val result = local { implicit ctx =>
      call(Request.PublicKey("Oh my..."))
    }
    assertSdkError(result)("Invalid hex string: Invalid character 'O' at position 0\r\nhex: [Oh my...]")
  }

  it should "factorize" in {
    val result = local { implicit ctx =>
      call(Request.Factorize("17ED48941A08F981"))
    }
    assertValue(result)(Result.Factors(Seq("494C553B", "53911073")))
  }

  it should "not factorize" in {
    val result = local { implicit ctx =>
      call(Request.Factorize("Gotcha!"))
    }
    assertSdkError(result)(
      "Invalid factorize challenge: invalid digit found in string\r\nchallenge: [Gotcha!]".stripMargin)
  }

  it should "generate_random_bytes" in {
    val result = local { implicit ctx =>
      call(Request.GenerateRandomBytes(32))
    }
    assertExpression(result)(_.bytes.length == 44)
  }

  it should "not generate_random_bytes" in {
    val result = local { implicit ctx =>
      call(Request.GenerateRandomBytes(-1))
    }
    assertSdkError(result)("Invalid parameters: invalid value: integer `-1`, expected usize at line 1 column 12\nparams: {\"length\":-1}")
  }

  it should "test_generate_random_sign_keys" in {
    val result = local { implicit ctx =>
      call(Request.GenerateRandomSignKeys)
    }
    assertExpression(result)(r => r.public.length == 64 && r.secret.length == 64 && r.public != r.secret)
  }

}
