package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding.Api.SdkCall

/**
  * Module proofs
  *
  * Module for proving data, retrieved from TONOS API.
  *
  * Please refer to the [[https://github.com/tonlabs/TON-SDK/blob/master/docs/mod_proofs.md SDK documentation]]
  * for the detailed description of individual functions and parameters
  *
  */
// scalafmt: { maxColumn = 300 }
object Proofs {

  private val module = "proofs"

  object Request {
    final case class ProofBlockData(block: Json)
    final case class ProofTransactionData(transaction: Json)
  }
  object Result {
    final case class Address(address: String)
    final case class Fee(fee: String)
    final case class Compressed(compressed: String)
    final case class Decompressed(decompressed: String)
    final case class AddressType(address_type: String)
  }

  import io.circe.generic.auto._

  implicit val proof_block_data       = new SdkCall[Request.ProofBlockData, Unit]       { override val function: String = s"$module.proof_block_data"       }
  implicit val proof_transaction_data = new SdkCall[Request.ProofTransactionData, Unit] { override val function: String = s"$module.proof_transaction_data" }
}
