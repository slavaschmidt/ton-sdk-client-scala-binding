package ton.sdk.client.modules

import ton.sdk.client.binding.Api.SdkCall

/**
  * Module utils
  *
  * Misc utility Functions.
  *
  * Please refer to the [[https://github.com/tonlabs/TON-SDK/blob/master/docs/mod_utils.md SDK documentation]]
  * for the detailed description of individual functions and parameters
  *
  */
// scalafmt: { maxColumn = 300 }
object Utils {

  private val module = "utils"

  final case class AddressStringFormat(`type`: String, url: Option[Boolean] = None, test: Option[Boolean] = None, bounce: Option[Boolean] = None)

  object AddressStringFormat {
    val accountId: AddressStringFormat                                                                    = AddressStringFormat("AccountId")
    val hex: AddressStringFormat                                                                          = AddressStringFormat("Hex")
    def base64(url: Boolean = false, test: Boolean = false, bounce: Boolean = false): AddressStringFormat = AddressStringFormat("Base64", Option(url), Option(test), Option(bounce))
  }

  object Request {
    final case class ConvertAddress(address: String, output_format: AddressStringFormat)
  }
  object Result {
    final case class Address(address: String)
  }

  import io.circe.generic.auto._

  implicit val convertAddress = new SdkCall[Request.ConvertAddress, Result.Address] { override val function: String = s"$module.convert_address" }

}
