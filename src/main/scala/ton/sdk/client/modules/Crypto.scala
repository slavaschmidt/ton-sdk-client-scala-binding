package ton.sdk.client.modules

import ton.sdk.client.binding.KeyPair
import ton.sdk.client.binding.Api.SdkCall

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
    final case object GenerateRandomSignKeys
    final case class PublicKey(public_key: String)
    final case class Factorize(composite: String)
    final case class GenerateRandomBytes(length: Int)
    final case class HdkeyDeriveFromXprv(xprv: String, child_index: Int, hardened: Boolean)
    final case class HdkeyDeriveFromXprvPath(xprv: String, path: String = DEFAULT_HDKEY_DERIVATION_PATH)
    final case class HdkeyXprvFromMnemonic(phrase: String)
    final case class HdkeySecretFromXprv(xprv: String)
    final case class HdkeyPublicFromXprv(xprv: String)
    final case class MnemonicWords(dictionary: Int = DEFAULT_MNEMONIC_DICTIONARY)
    final case class MnemonicFromRandom(dictionary: Int, word_count: Int)
    final case class MnemonicFromEntropy(entropy: String)
    final case class MnemonicVerify(phrase: String, word_count: Int = DEFAULT_MNEMONIC_WORD_COUNT, dictionary: Int = DEFAULT_MNEMONIC_DICTIONARY)
    final case class MnemonicDeriveSignKeys(
      phrase: String,
      path: String = DEFAULT_HDKEY_DERIVATION_PATH,
      word_count: Int = DEFAULT_MNEMONIC_WORD_COUNT,
      dictionary: Int = DEFAULT_MNEMONIC_DICTIONARY
    )
    final case class ModularPower(base: String, exponent: String, modulus: String)
    final case object NaclBoxKeyPair
    final case class NaclBoxKeyPairFromSecretKey(secret: String)
    final case class NaclBox(decrypted: String, nonce: String, their_public: String, secret: String)
    final case class NaclBoxOpen(encrypted: String, nonce: String, their_public: String, secret: String)
    final case class NaclSecretBox(decrypted: String, nonce: String, key: String)
    final case class NaclSecretBoxOpen(encrypted: String, nonce: String, key: String)
    final case class NaclSign(unsigned: String, secret: String)
    final case class NaclSignOpen(signed: String, public: String)
    final case class NaclSignDetached(unsigned: String, secret: String)
    final case class NaclSignKeypairFromSecretKey(secret: String)
    final case class Scrypt(password: String, salt: String, log_n: Int, r: Int, p: Int, dk_len: Int)
    final case class Sha256(data: String)
    final case class Sha512(data: String)
    final case class TonCrc16(data: String)
    final case class Sign(unsigned: String, keys: KeyPair)
    final case class VerifySignature(signed: String, public: String)
  }

  object Result {
    final case class TonPublicKey(ton_public_key: String)
    final case class Factors(factors: Seq[String])
    final case class Bytes(bytes: String)
    final case class Xprv(xprv: String)
    final case class SecretKey(secret: String)
    final case class PublicKey(public: String)
    final case class MnemonicWords(words: String) {
      def wordCount: Int = words.split(" ").length
    }
    final case class MnemonicPhrase(phrase: String) {
      def wordCount: Int = phrase.split(" ").length
    }
    final case class Validity(valid: Boolean)
    final case class PublicSafe(public_safe: String)
    final case class ModularPower(modular_power: String)
    final case class Encrypted(encrypted: String)
    final case class Decrypted(decrypted: String)
    final case class Signed(signed: String, signature: Option[String])
    final case class Unsigned(unsigned: String)
    final case class Signature(signature: String)
    final case class Key(key: String)
    final case class Hash(hash: String)
    final case class Crc(crc: Long)
  }

  import io.circe.generic.auto._

  implicit val convertPublicKeyToTonSafeFormat = new SdkCall[Request.PublicKey, Result.TonPublicKey] {
    override val function: String = s"$prefix.convert_public_key_to_ton_safe_format"
  }
  implicit val factorize           = new SdkCall[Request.Factorize, Result.Factors]         { override val function: String = s"$prefix.factorize"             }
  implicit val generateRandomBytes = new SdkCall[Request.GenerateRandomBytes, Result.Bytes] { override val function: String = s"$prefix.generate_random_bytes" }
  implicit val generateRandomSignKeys = new SdkCall[Request.GenerateRandomSignKeys.type, KeyPair] {
    override val function: String = s"$prefix.generate_random_sign_keys"
  }
  implicit val hdkeyDeriveFromXprv = new SdkCall[Request.HdkeyDeriveFromXprv, Result.Xprv] { override val function: String = s"$prefix.hdkey_derive_from_xprv" }
  implicit val hdkeyDeriveFromXprvPath = new SdkCall[Request.HdkeyDeriveFromXprvPath, Result.Xprv] {
    override val function: String = s"$prefix.hdkey_derive_from_xprv_path"
  }
  implicit val hdkeyPublicFromXprv   = new SdkCall[Request.HdkeyPublicFromXprv, Result.PublicKey] { override val function: String = s"$prefix.hdkey_public_from_xprv"   }
  implicit val hdkeySecretFromXprv   = new SdkCall[Request.HdkeySecretFromXprv, Result.SecretKey] { override val function: String = s"$prefix.hdkey_secret_from_xprv"   }
  implicit val hdkeyXprvFromMnemonic = new SdkCall[Request.HdkeyXprvFromMnemonic, Result.Xprv]    { override val function: String = s"$prefix.hdkey_xprv_from_mnemonic" }

  implicit val mnemonicWords          = new SdkCall[Request.MnemonicWords, Result.MnemonicWords]        { override val function: String = s"$prefix.mnemonic_words"            }
  implicit val mnemonicFromRandom     = new SdkCall[Request.MnemonicFromRandom, Result.MnemonicPhrase]  { override val function: String = s"$prefix.mnemonic_from_random"      }
  implicit val mnemonicFromEntropy    = new SdkCall[Request.MnemonicFromEntropy, Result.MnemonicPhrase] { override val function: String = s"$prefix.mnemonic_from_entropy"     }
  implicit val mnemonicVerify         = new SdkCall[Request.MnemonicVerify, Result.Validity]            { override val function: String = s"$prefix.mnemonic_verify"           }
  implicit val mnemonicDeriveSignKeys = new SdkCall[Request.MnemonicDeriveSignKeys, Result.PublicKey]   { override val function: String = s"$prefix.mnemonic_derive_sign_keys" }
  implicit val modularPower           = new SdkCall[Request.ModularPower, Result.ModularPower]          { override val function: String = s"$prefix.modular_power"             }
  implicit val naclBoxKeypair         = new SdkCall[Request.NaclBoxKeyPair.type, KeyPair]               { override val function: String = s"$prefix.nacl_box_keypair"          }
  implicit val naclBoxKeypairFromSecretKey = new SdkCall[Request.NaclBoxKeyPairFromSecretKey, KeyPair] {
    override val function: String = s"$prefix.nacl_box_keypair_from_secret_key"
  }

  implicit val naclBox           = new SdkCall[Request.NaclBox, Result.Encrypted]           { override val function: String = s"$prefix.nacl_box"             }
  implicit val naclBoxOpen       = new SdkCall[Request.NaclBoxOpen, Result.Decrypted]       { override val function: String = s"$prefix.nacl_box_open"        }
  implicit val naclSecretBox     = new SdkCall[Request.NaclSecretBox, Result.Encrypted]     { override val function: String = s"$prefix.nacl_secret_box"      }
  implicit val naclSecretBoxOpen = new SdkCall[Request.NaclSecretBoxOpen, Result.Decrypted] { override val function: String = s"$prefix.nacl_secret_box_open" }
  implicit val naclSign          = new SdkCall[Request.NaclSign, Result.Signed]             { override val function: String = s"$prefix.nacl_sign"            }
  implicit val naclSignOpen      = new SdkCall[Request.NaclSignOpen, Result.Unsigned]       { override val function: String = s"$prefix.nacl_sign_open"       }
  implicit val naclSignDetached  = new SdkCall[Request.NaclSignDetached, Result.Signature]  { override val function: String = s"$prefix.nacl_sign_detached"   }
  implicit val naclSignKeypairFromSecretKey = new SdkCall[Request.NaclSignKeypairFromSecretKey, Result.PublicKey] {
    override val function: String = s"$prefix.nacl_sign_keypair_from_secret_key"
  }
  implicit val scrypt          = new SdkCall[Request.Scrypt, Result.Key]               { override val function: String = s"$prefix.scrypt"           }
  implicit val sha256          = new SdkCall[Request.Sha256, Result.Hash]              { override val function: String = s"$prefix.sha256"           }
  implicit val sha512          = new SdkCall[Request.Sha512, Result.Hash]              { override val function: String = s"$prefix.sha512"           }
  implicit val tonCrc16        = new SdkCall[Request.TonCrc16, Result.Crc]             { override val function: String = s"$prefix.ton_crc16"        }
  implicit val verifySignature = new SdkCall[Request.VerifySignature, Result.Unsigned] { override val function: String = s"$prefix.verify_signature" }
  implicit val sign            = new SdkCall[Request.Sign, Result.Signed]              { override val function: String = s"$prefix.sign"             }

}
