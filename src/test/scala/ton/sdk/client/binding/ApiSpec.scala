package ton.sdk.client.binding

import io.circe.Json
import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import ton.sdk.client.binding.Api._

class ApiSpec extends AsyncFlatSpec {

  behavior of "Api"

  "testing response codes" should "improve coverage" in {
    ResponseTypeResult.code shouldEqual 0L
    ResponseTypeError.code shouldEqual 1L
    ResponseTypeNop.code shouldEqual 2L
  }

  "testing string representation" should "further improve coverage" in {
    val data  = Json.fromString("This can only happen if client returns malformed json")
    val error = new SdkClientError(HTTP_REQUEST_CREATE_ERROR, "Uh-Oh", data)
    val str   = new SdkError[Unit](error).toString
    str shouldEqual "ton.sdk.client.binding.Api$SdkClientError: Uh-Oh"
  }

  "testing parsing error" should "improve coverage even more" in {
    val error      = SdkResultOrError.fromJsonWrapped[Int]("You shall not pass!!!")
    val expected   = "expected json value got 'You sh...' (line 1, column 1)"
    error.failed.get.getMessage shouldBe expected
  }

}
