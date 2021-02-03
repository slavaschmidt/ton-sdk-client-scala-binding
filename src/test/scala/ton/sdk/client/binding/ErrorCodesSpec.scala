package ton.sdk.client.binding

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import ton.sdk.client.binding.AbiErrors._
import ton.sdk.client.binding.BocErrors._
import ton.sdk.client.binding.ClientErrors._
import ton.sdk.client.binding.CryptoErrors._
import ton.sdk.client.binding.DebotErrors.DebotStartFailed
import ton.sdk.client.binding.NetErrors.QueryFailed
import ton.sdk.client.binding.ProcessingErrors.MessageAlreadyExpired
import ton.sdk.client.binding.TvmErrors.CanNotReadTransaction

class ErrorCodesSpec extends AsyncFlatSpec {

  behavior of "ErrorCodes"

  "error codes" should "be defined" in {
    NotImplemented.code shouldEqual 1L
    InvalidPublicKey.code shouldEqual 100L
    InvalidBoc.code shouldEqual 201L
    RequiredAddressMissingForEncodeMessage.code shouldEqual 301L
    CanNotReadTransaction.code shouldEqual 401L
    MessageAlreadyExpired.code shouldEqual 501L
    QueryFailed.code shouldEqual 601L
    DebotStartFailed.code shouldEqual 801L
  }

}
