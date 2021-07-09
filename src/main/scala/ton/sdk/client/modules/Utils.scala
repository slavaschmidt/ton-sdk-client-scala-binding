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
    final case class CalcStorageFee(account: String, period: Int)
    final case class CompressZstd(uncompressed: String, level: Option[Int]) // 1 to 21
    final case class DecompressZstd(compressed: String)                     // 1 to 21
    final case class GetAddressType(address: String)
  }
  object Result {
    final case class Address(address: String)
    final case class Fee(fee: String)
    final case class Compressed(compressed: String)
    final case class Decompressed(decompressed: String)
    final case class AddressType(address_type: String)
  }

  import io.circe.generic.auto._

  implicit val convertAddress = new SdkCall[Request.ConvertAddress, Result.Address]      { override val function: String = s"$module.convert_address"  }
  implicit val calcStorageFee = new SdkCall[Request.CalcStorageFee, Result.Fee]          { override val function: String = s"$module.calc_storage_fee" }
  implicit val compressZstd   = new SdkCall[Request.CompressZstd, Result.Compressed]     { override val function: String = s"$module.compress_zstd"    }
  implicit val decompressZstd = new SdkCall[Request.DecompressZstd, Result.Decompressed] { override val function: String = s"$module.decompress_zstd"  }
  implicit val getAddressType = new SdkCall[Request.GetAddressType, Result.AddressType]  { override val function: String = s"$module.get_address_type" }
}
