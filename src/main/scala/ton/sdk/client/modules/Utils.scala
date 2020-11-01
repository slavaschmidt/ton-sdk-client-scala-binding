package ton.sdk.client.modules

import ton.sdk.client.modules.Api.SdkCall

object Utils {

  private val prefix = "utils"
  final case class AddressOutputFormat(`type`: String, url: Option[Boolean] = None, test: Option[Boolean] = None, bounce: Option[Boolean] = None)

  object Types {
    val accountId: AddressOutputFormat = AddressOutputFormat("AccountId")
    val hex: AddressOutputFormat = AddressOutputFormat("Hex")
    def base64(url: Boolean = false, test: Boolean = false, bounce: Boolean = false): AddressOutputFormat =
      AddressOutputFormat("Base64", Option(url), Option(test), Option(bounce))
  }
  object Request {
    case class ConvertAddress(address: String, output_format: AddressOutputFormat)
  }
  object Result {
    case class ConvertedAddress(address: String)
  }

  import io.circe.generic.auto._

  implicit val convertAddress = new SdkCall[Request.ConvertAddress, Result.ConvertedAddress] {
    override val functionName: String = s"$prefix.convert_address"
  }

}
