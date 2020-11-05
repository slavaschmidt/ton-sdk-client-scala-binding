package ton.sdk.client.modules

import ton.sdk.client.modules.Api.SdkCall

object Crypto {

  val prefix = "crypto"

  val DEFAULT_MNEMONIC_DICTIONARY   = 1
  val DEFAULT_MNEMONIC_WORD_COUNT   = 12
  val DEFAULT_HDKEY_DERIVATION_PATH = "m/44'/396'/0'/0/0"

  val MNEMONIC_DICTIONARY_TON                 = 0
  val MNEMONIC_DICTIONARY_ENGLISH             = 1
  val MNEMONIC_DICTIONARY_CHINESE_SIMPLIFIED  = 2
  val MNEMONIC_DICTIONARY_CHINESE_TRADITIONAL = 3
  val MNEMONIC_DICTIONARY_FRENCH              = 4
  val MNEMONIC_DICTIONARY_ITALIAN             = 5
  val MNEMONIC_DICTIONARY_JAPANESE            = 6
  val MNEMONIC_DICTIONARY_KOREAN              = 7
  val MNEMONIC_DICTIONARY_SPANISH             = 8

  object Request {
    case object GenerateRandomSignKeys
    case class PublicKey(public_key: String)
    case class Factorize(composite: String)
    case class GenerateRandomBytes(length: Int)
    case class HdkeyDeriveFromXprv(xprv: String, child_index: Int, hardened: Boolean)
    case class HdkeyDeriveFromXprvPath(xprv: String, path: String = DEFAULT_HDKEY_DERIVATION_PATH)
    case class HdkeyXprvFromMnemonic(phrase: String)
    case class HdkeySecretFromXprv(xprv: String)
    case class HdkeyPublicFromXprv(xprv: String)
    case class MnemonicWords(dictionary: Int = DEFAULT_MNEMONIC_DICTIONARY)
    case class MnemonicFromRandom(dictionary: Int, word_count: Int)
    case class MnemonicFromEntropy(entropy: String)
    case class MnemonicVerify(phrase: String, word_count: Int = DEFAULT_MNEMONIC_WORD_COUNT, dictionary: Int = DEFAULT_MNEMONIC_DICTIONARY)
    case class MnemonicDeriveSignKeys(
      phrase: String,
      path: String = DEFAULT_HDKEY_DERIVATION_PATH,
      word_count: Int = DEFAULT_MNEMONIC_WORD_COUNT,
      dictionary: Int = DEFAULT_MNEMONIC_DICTIONARY
    )
    case class ModularPower(base: String, exponent: String, modulus: String)
    case object NaclBoxKeyPair
    case class NaclBoxKeyPairFromSecretKey(secret: String)
    case class NaclBox(decrypted: String, nonce: String, their_public: String, secret: String)
    case class NaclBoxOpen(encrypted: String, nonce: String, their_public: String, secret: String)
    case class NaclSecretBox(decrypted: String, nonce: String, key: String)
    case class NaclSecretBoxOpen(encrypted: String, nonce: String, key: String)
    case class NaclSign(unsigned: String, secret: String)
    case class NaclSignOpen(signed: String, public: String)
    case class NaclSignDetached(unsigned: String, secret: String)
  }

  object Result {
    case class TonPublicKey(ton_public_key: String)
    case class Factors(factors: Seq[String])
    case class Bytes(bytes: String)
    case class SignKeys(public: String, secret: String)
    case class Xprv(xprv: String)
    case class SecretKey(secret: String)
    case class PublicKey(public: String)
    case class MnemonicWords(words: String) {
      def wordCount: Int = words.split(" ").length
    }
    case class MnemonicPhrase(phrase: String) {
      def wordCount: Int = phrase.split(" ").length
    }
    case class Validity(valid: Boolean)
    case class PublicSafe(public_safe: String)
    case class ModularPower(modular_power: String)
    case class Encrypted(encrypted: String)
    case class Decrypted(decrypted: String)
    case class Signed(signed: String)
    case class Unsigned(unsigned: String)
    case class Signature(signature: String)
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
  implicit val hdkeyPublicFromXprv   = new SdkCall[Request.HdkeyPublicFromXprv, Result.PublicKey] { override val functionName: String = s"$prefix.hdkey_public_from_xprv"   }
  implicit val hdkeySecretFromXprv   = new SdkCall[Request.HdkeySecretFromXprv, Result.SecretKey] { override val functionName: String = s"$prefix.hdkey_secret_from_xprv"   }
  implicit val hdkeyXprvFromMnemonic = new SdkCall[Request.HdkeyXprvFromMnemonic, Result.Xprv]    { override val functionName: String = s"$prefix.hdkey_xprv_from_mnemonic" }

  implicit val mnemonicWords          = new SdkCall[Request.MnemonicWords, Result.MnemonicWords]        { override val functionName: String = s"$prefix.mnemonic_words"            }
  implicit val mnemonicFromRandom     = new SdkCall[Request.MnemonicFromRandom, Result.MnemonicPhrase]  { override val functionName: String = s"$prefix.mnemonic_from_random"      }
  implicit val mnemonicFromEntropy    = new SdkCall[Request.MnemonicFromEntropy, Result.MnemonicPhrase] { override val functionName: String = s"$prefix.mnemonic_from_entropy"     }
  implicit val mnemonicVerify         = new SdkCall[Request.MnemonicVerify, Result.Validity]            { override val functionName: String = s"$prefix.mnemonic_verify"           }
  implicit val mnemonicDeriveSignKeys = new SdkCall[Request.MnemonicDeriveSignKeys, Result.PublicKey]   { override val functionName: String = s"$prefix.mnemonic_derive_sign_keys" }
  implicit val modularPower           = new SdkCall[Request.ModularPower, Result.ModularPower]          { override val functionName: String = s"$prefix.modular_power"             }
  implicit val naclBoxKeypair         = new SdkCall[Request.NaclBoxKeyPair.type, Result.SignKeys]       { override val functionName: String = s"$prefix.nacl_box_keypair"          }
  implicit val naclBoxKeypairFromSecretKey = new SdkCall[Request.NaclBoxKeyPairFromSecretKey, Result.SignKeys] {
    override val functionName: String = s"$prefix.nacl_box_keypair_from_secret_key"
  }

  implicit val naclBox           = new SdkCall[Request.NaclBox, Result.Encrypted]           { override val functionName: String = s"$prefix.nacl_box"             }
  implicit val naclBoxOpen       = new SdkCall[Request.NaclBoxOpen, Result.Decrypted]       { override val functionName: String = s"$prefix.nacl_box_open"        }
  implicit val naclSecretBox     = new SdkCall[Request.NaclSecretBox, Result.Encrypted]     { override val functionName: String = s"$prefix.nacl_secret_box"      }
  implicit val naclSecretBoxOpen = new SdkCall[Request.NaclSecretBoxOpen, Result.Decrypted] { override val functionName: String = s"$prefix.nacl_secret_box_open" }
  implicit val naclSign          = new SdkCall[Request.NaclSign, Result.Signed]             { override val functionName: String = s"$prefix.nacl_sign"            }
  implicit val naclSignOpen      = new SdkCall[Request.NaclSignOpen, Result.Unsigned]       { override val functionName: String = s"$prefix.nacl_sign_open"       }
  implicit val naclSignDetached  = new SdkCall[Request.NaclSignDetached, Result.Signature]  { override val functionName: String = s"$prefix.nacl_sign_detached"   }
//  implicit val nacl_sign_keypair_from_secret_key = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.nacl_sign_keypair_from_secret_key" }

//  implicit val scrypt = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.scrypt" }
//  implicit val sha256 = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.sha256" }
//  implicit val sha512 = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.sha512" }
//  implicit val sign = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.sign" }
//  implicit val ton_crc16 = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.ton_crc16" }
//  implicit val verify_signature = new SdkCall[Request.,Result.] { override val functionName: String = s"$prefix.verify_signature" }

}
