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

  val mnemonic = "abuse boss fly battle rubber wasp afraid hamster guide essence vibrant tattoo"
  val masterXprv = "xprv9s21ZrQH143K25JhKqEwvJW7QAiVvkmi4WRenBZanA6kxHKtKAQQKwZG65kCyW5jWJ8NY9e3GkRoistUjjcpHNsGBUv94istDPXvqGNuWpC"

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

  it should "generate_random_sign_keys" in {
    val result = local { implicit ctx =>
      call(Request.GenerateRandomSignKeys)
    }
    assertExpression(result)(r => r.public.length == 64 && r.secret.length == 64 && r.public != r.secret)
  }

  it should "hdkey_derive_from_xprv" in {
    val result = local { implicit ctx =>
      call(Request.HdkeyDeriveFromXprv(masterXprv, 0, false))
    }
    assertValue(result)(Result.Xprv("xprv9uZwtSeoKf1swgAkVVCEUmC2at6t7MCJoHnBbn1MWJZyxQ4cySkVXPyNh7zjf9VjsP4vEHDDD2a6R35cHubg4WpzXRzniYiy8aJh1gNnBKv"))
  }

  it should "not hdkey_derive_from_xprv" in {
    val result = local { implicit ctx =>
      call(Request.HdkeyDeriveFromXprv(masterXprv, -10, false))
    }
    assertSdkError(result)("Invalid parameters: invalid value: integer `-10`, expected u32 at line 1 column 139\nparams: {\"xprv\":\"xprv9s21ZrQH143K25JhKqEwvJW7QAiVvkmi4WRenBZanA6kxHKtKAQQKwZG65kCyW5jWJ8NY9e3GkRoistUjjcpHNsGBUv94istDPXvqGNuWpC\",\"child_index\":-10,\"hardened\":false}")
  }

  it should "hdkey_derive_from_xprv_path" in {
    val result = local { implicit ctx =>
      call(Request.HdkeyDeriveFromXprvPath(masterXprv, "m/44'/60'/0'/0'"))
    }
    assertValue(result)(Result.Xprv("xprvA1KNMo63UcGjmDF1bX39Cw2BXGUwrwMjeD5qvQ3tA3qS3mZQkGtpf4DHq8FDLKAvAjXsYGLHDP2dVzLu9ycta8PXLuSYib2T3vzLf3brVgZ"))
  }

  it should "not hdkey_derive_from_xprv_path" in {
    val result = local { implicit ctx =>
      call(Request.HdkeyDeriveFromXprvPath(masterXprv, "m/"))
    }
    assertSdkError(result)("Invalid bip32 derive path: m/")
  }

  it should "hdkey_xprv_from_mnemonic" in {
    val result = local { implicit ctx =>
      call(Request.HdkeyXprvFromMnemonic(mnemonic))
    }
    assertValue(result)(Result.Xprv(masterXprv))
  }

  it should "not hdkey_xprv_from_mnemonic" in {
    val result = local { implicit ctx =>
      call(Request.HdkeyXprvFromMnemonic("nooooooooo"))
    }
    assertSdkError(result)("Invalid bip39 phrase: nooooooooo")
  }

  it should "hdkey_secret_from_xprv" in {
    val result = local { implicit ctx =>
      call(Request.HdkeySecretFromXprv(masterXprv))
    }
    assertValue(result)(Result.SecretKey("0c91e53128fa4d67589d63a6c44049c1068ec28a63069a55ca3de30c57f8b365"))
  }

  it should "not hdkey_secret_from_xprv" in {
    val result = local { implicit ctx =>
      call(Request.HdkeySecretFromXprv("Noooooo"))
    }
    assertSdkError(result)("Invalid bip32 key: Noooooo")
  }

  it should "hdkey_public_from_xprv" in {
    val result = local { implicit ctx =>
      call(Request.HdkeyPublicFromXprv(masterXprv))
    }
    assertValue(result)(Result.PublicKey("02a8eb63085f73c33fa31b4d1134259406347284f8dab6fc68f4bf8c96f6c39b75"))
  }

  it should "not hdkey_public_from_xprv" in {
    val result = local { implicit ctx =>
      call(Request.HdkeyPublicFromXprv(mnemonic))
    }
    assertSdkError(result)(s"Invalid bip32 key: $mnemonic")
  }

}
