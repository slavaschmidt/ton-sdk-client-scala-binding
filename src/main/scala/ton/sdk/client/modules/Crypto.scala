package ton.sdk.client.modules

import ton.sdk.client.modules.Api.SdkCall

object Crypto {

  val prefix = "crypto"

  object Request {
    case object GenerateRandomSignKeys
    case class PublicKey(public_key: String)
    case class Factorize(composite: String)
    case class GenerateRandomBytes(length: Int)
    case class HdkeyDeriveFromXprv(xprv: String, child_index: Int, hardened: Boolean)
    case class HdkeyDeriveFromXprvPath(xprv: String, path: String)
    case class HdkeyXprvFromMnemonic(phrase: String)
    case class HdkeySecretFromXprv(xprv: String)
    case class HdkeyPublicFromXprv(xprv: String)
  }
  object Result {
    case class TonPublicKey(ton_public_key: String)
    case class Factors(factors: Seq[String])
    case class Bytes(bytes: String)
    case class SignKeys(public: String, secret: String)
    case class Xprv(xprv: String)
    case class SecretKey(secret: String)
    case class PublicKey(public: String)
  }

  import io.circe.generic.auto._

  implicit val convertPublicKeyToTonSafeFormat = new SdkCall[Request.PublicKey, Result.TonPublicKey] {
    override val functionName: String = s"$prefix.convert_public_key_to_ton_safe_format"
  }
  implicit val factorize           = new SdkCall[Request.Factorize, Result.Factors]         { override val functionName: String = s"$prefix.factorize"             }
  implicit val generateRandomBytes = new SdkCall[Request.GenerateRandomBytes, Result.Bytes] { override val functionName: String = s"$prefix.generate_random_bytes" }
  implicit val generateRandomSignKeys = new SdkCall[Request.GenerateRandomSignKeys.type, Result.SignKeys] {
    override val functionName: String = s"$prefix.generate_random_sign_keys"
  }
  implicit val hdkeyDeriveFromXprv = new SdkCall[Request.HdkeyDeriveFromXprv, Result.Xprv] { override val functionName: String = s"$prefix.hdkey_derive_from_xprv" }
  implicit val hdkeyDeriveFromXprvPath = new SdkCall[Request.HdkeyDeriveFromXprvPath, Result.Xprv] {
    override val functionName: String = s"$prefix.hdkey_derive_from_xprv_path"
  }

  implicit val hdkeyPublicFromXprv   = new SdkCall[Request.HdkeyPublicFromXprv, Result.PublicKey]   { override val functionName: String = s"$prefix.hdkey_public_from_xprv"   }
  implicit val hdkeySecretFromXprv   = new SdkCall[Request.HdkeySecretFromXprv, Result.SecretKey]   { override val functionName: String = s"$prefix.hdkey_secret_from_xprv"   }
  implicit val hdkeyXprvFromMnemonic = new SdkCall[Request.HdkeyXprvFromMnemonic, Result.Xprv] { override val functionName: String = s"$prefix.hdkey_xprv_from_mnemonic" }

  //  implicit val mnemonic_derive_sign_keys = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.mnemonic_derive_sign_keys" }
//  implicit val mnemonic_from_entropy = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.mnemonic_from_entropy" }
//  implicit val mnemonic_from_random = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.mnemonic_from_random" }
//  implicit val mnemonic_verify = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.mnemonic_verify" }
//  implicit val mnemonic_words = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.mnemonic_words" }
//  implicit val modular_power = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.modular_power" }
//  implicit val nacl_box = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.nacl_box" }
//  implicit val nacl_box_keypair = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.nacl_box_keypair" }
//  implicit val nacl_box_keypair_from_secret_key = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.nacl_box_keypair_from_secret_key" }
//  implicit val nacl_box_open = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.nacl_box_open" }
//  implicit val nacl_secret_box = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.nacl_secret_box" }
//  implicit val nacl_secret_box_open = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.nacl_secret_box_open" }
//  implicit val nacl_sign = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.nacl_sign" }
//  implicit val nacl_sign_detached = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.nacl_sign_detached" }
//  implicit val nacl_sign_keypair_from_secret_key = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.nacl_sign_keypair_from_secret_key" }
//  implicit val nacl_sign_open = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.nacl_sign_open" }
//  implicit val scrypt = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.scrypt" }
//  implicit val sha256 = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.sha256" }
//  implicit val sha512 = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.sha512" }
//  implicit val sign = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.sign" }
//  implicit val ton_crc16 = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.ton_crc16" }
//  implicit val verify_signature = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.verify_signature" }

}
