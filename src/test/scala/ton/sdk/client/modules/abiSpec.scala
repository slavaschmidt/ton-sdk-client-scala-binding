package ton.sdk.client.modules

import io.circe.JsonObject
import io.circe.literal._
import io.circe.syntax._
import org.scalatest.flatspec._
import ton.sdk.client.binding.Context._
import ton.sdk.client.binding._
import ton.sdk.client.modules.Abi._

import java.io.File
import java.nio.file.Files
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scala.util.Try

class SyncAbiSpec extends AbiSpec[Try] {
  implicit override val ef: Context.Effect[Try] = tryEffect
}

class AsyncAbiSpec extends AbiSpec[Future] {
  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val ef: Context.Effect[Future]         = futureEffect

  it should "decode_initial_data and update_initial_data" in {
    val initialData = JsonObject("a"-> 123.asJson, "s" -> "some string".asJson).asJson
    val expectedUpdated = "te6ccgEBBwEARwABAcABAgPPoAQCAQFIAwAWc29tZSBzdHJpbmcCASAGBQADHuAAQQiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIiIoA=="
    val pubKey = asHex(new String(Array.fill[Byte](32)(0x22)))
    val initialPubkey = asHex(new String(Array.fill[Byte](64)(0)))
    println(initialPubkey)
    val a = local { implicit ctx =>
      for {
        data <- call(Boc.Request.DecodeTvc(tvc("t24_initdata"), None))
        result <- call(Request.DecodeInitialData(Option(abiJson("t24_initdata")), data.data.get))
        updated <- call(Request.UpdateInitialData(Option(abiJson("t24_initdata")), data.data.get, Some(initialData), Option(pubKey), None))
      } yield result.initial_pubkey == initialPubkey &&
        result.initial_data.contains(JsonObject.empty.asJson) &&
        updated.data == expectedUpdated
    }
    assertValue(a)(true)
  }

}

abstract class AbiSpec[T[_]] extends AsyncFlatSpec with SdkAssertions[T] {

  behavior of "Abi"

  implicit val ef: Effect[T]

  private val keyPair =
    KeyPair(public = "4c7c408ff1ddebb8d6405ee979c716a14fdd6cc08124107a61d3c25597099499", secret = "cc8929d635719612a9478b9cd17675a39cfad52d8959e8a177389b8c0b9122a7")
  private val abi          = AbiJson.fromResource("Events.abi.json", getClass.getClassLoader).toOption.get
  private val tvcSrc       = Files.readAllBytes(new File(getClass.getClassLoader.getResource("Events.tvc").getFile).toPath)
  private val tvc          = base64(tvcSrc)
  private val eventsTime   = 1599458364291L
  private val eventsExpire = 1599458404

  private val encodedMessage =
    "te6ccgEBAwEAvAABRYgAC31qq9KF9Oifst6LU9U6FQSQQRlCSEMo+A3LN5MvphIMAQHhrd/b+MJ5Za+AygBc5qS/dVIPnqxCsM9PvqfVxutK+lnQEKzQoRTLYO6+jfM8TF4841bdNjLQwIDWL4UVFdxIhdMfECP8d3ruNZAXul5xxahT91swIEkEHph08JVlwmUmQAAAXRnJcuDX1XMZBW+LBKACAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=="

  private val expectedSignedMessage =
    "te6ccgECGAEAA6wAA0eIAAt9aqvShfTon7Lei1PVOhUEkEEZQkhDKPgNyzeTL6YSEbAHAgEA4bE5Gr3mWwDtlcEOWHr6slWoyQlpIWeYyw/00eKFGFkbAJMMFLWnu0mq4HSrPmktmzeeAboa4kxkFymCsRVt44dTHxAj/Hd67jWQF7peccWoU/dbMCBJBB6YdPCVZcJlJkAAAF0ZyXLg19VzGRotV8/gAQHAAwIDzyAGBAEB3gUAA9AgAEHaY+IEf47vXcayAvdLzji1Cn7rZgQJIIPTDp4SrLhMpMwCJv8A9KQgIsABkvSg4YrtU1gw9KEKCAEK9KQg9KEJAAACASANCwHI/38h7UTQINdJwgGOENP/0z/TANF/+GH4Zvhj+GKOGPQFcAGAQPQO8r3XC//4YnD4Y3D4Zn/4YeLTAAGOHYECANcYIPkBAdMAAZTT/wMBkwL4QuIg+GX5EPKoldMAAfJ64tM/AQwAao4e+EMhuSCfMCD4I4ED6KiCCBt3QKC53pL4Y+CANPI02NMfAfgjvPK50x8B8AH4R26S8jzeAgEgEw4CASAQDwC9uotV8/+EFujjXtRNAg10nCAY4Q0//TP9MA0X/4Yfhm+GP4Yo4Y9AVwAYBA9A7yvdcL//hicPhjcPhmf/hh4t74RvJzcfhm0fgA+ELIy//4Q88LP/hGzwsAye1Uf/hngCASASEQDluIAGtb8ILdHCfaiaGn/6Z/pgGi//DD8M3wx/DFvfSDK6mjofSBv6PwikDdJGDhvfCFdeXAyfABkZP2CEGRnwoRnRoIEB9AAAAAAAAAAAAAAAAAAIGeLZMCAQH2AGHwhZGX//CHnhZ/8I2eFgGT2qj/8M8ADFuZPCot8ILdHCfaiaGn/6Z/pgGi//DD8M3wx/DFva4b/yupo6Gn/7+j8AGRF7gAAAAAAAAAAAAAAAAhni2fA58jjyxi9EOeF/+S4/YAYfCFkZf/8IeeFn/wjZ4WAZPaqP/wzwAgFIFxQBCbi3xYJQFQH8+EFujhPtRNDT/9M/0wDRf/hh+Gb4Y/hi3tcN/5XU0dDT/9/R+ADIi9wAAAAAAAAAAAAAAAAQzxbPgc+Rx5YxeiHPC//JcfsAyIvcAAAAAAAAAAAAAAAAEM8Wz4HPklb4sEohzwv/yXH7ADD4QsjL//hDzws/+EbPCwDJ7VR/FgAE+GcActxwItDWAjHSADDcIccAkvI74CHXDR+S8jzhUxGS8jvhwQQighD////9vLGS8jzgAfAB+EdukvI83g=="

  it should "decode_message Input" in {
    val result = local { implicit ctx =>
      call(Request.DecodeMessage(abi, encodedMessage))
    }
    val expectedValue   = json"""{"id": "0x0000000000000000000000000000000000000000000000000000000000000000"}"""
    val header          = FunctionHeader(Option(1599458404), Option(BigInt(1599458364291L)), Option("4c7c408ff1ddebb8d6405ee979c716a14fdd6cc08124107a61d3c25597099499"))
    val expectedMessage = Result.DecodedMessageBody(MessageBodyType.input, "returnValue", Option(expectedValue), Option(header))
    ef.unsafeGet(ef.map(result)(assertResult(expectedMessage)))
  }

  it should "decode_message Event" in {
    val message = "te6ccgEBAQEAVQAApeACvg5/pmQpY4m61HmJ0ne+zjHJu3MNG8rJxUDLbHKBu/AAAAAAAAAMJL6z6ro48sYvAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABA"
    val result = local { implicit ctx =>
      call(Request.DecodeMessage(abi, message))
    }

    val expectedValue   = json"""{"id": "0x0000000000000000000000000000000000000000000000000000000000000000"}"""
    val expectedMessage = Result.DecodedMessageBody(MessageBodyType.event, "EventThrown", Option(expectedValue), None)
    ef.unsafeGet(ef.map(result)(assertResult(expectedMessage)))
  }

  it should "decode_message Output" in {
    val message = "te6ccgEBAQEAVQAApeACvg5/pmQpY4m61HmJ0ne+zjHJu3MNG8rJxUDLbHKBu/AAAAAAAAAMKr6z6rxK3xYJAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABA"
    val result = local { implicit ctx =>
      call(Request.DecodeMessage(abi, message))
    }

    val expectedValue   = json"""{"value0": "0x0000000000000000000000000000000000000000000000000000000000000000"}"""
    val expectedMessage = Result.DecodedMessageBody(MessageBodyType.output, "returnValue", Option(expectedValue), None)
    ef.unsafeGet(ef.map(result)(assertResult(expectedMessage)))
  }

  it should "not decode_message" in {
    val result = local { implicit ctx =>
      call(Request.DecodeMessage(abi, "Oh Weh"))
    }
    assertSdkError(result)("Message can't be decoded: Invalid BOC: error decode message BOC base64: Invalid byte 32, offset 2.")
  }

  it should "decode_message_body" in {
    val result = local { implicit ctx =>
      ef.flatMap(call(Boc.Request.ParseMessage(encodedMessage))) { parsed =>
        call(Request.DecodeMessageBody(abi, parsed.parsed.body.get, is_internal = false))
      }
    }

    val expectedValue   = json"""{"id": "0x0000000000000000000000000000000000000000000000000000000000000000"}"""
    val header          = FunctionHeader(Option(1599458404), Option(BigInt(1599458364291L)), Option("4c7c408ff1ddebb8d6405ee979c716a14fdd6cc08124107a61d3c25597099499"))
    val expectedMessage = Result.DecodedMessageBody(MessageBodyType.input, "returnValue", Option(expectedValue), Option(header))
    ef.unsafeGet(ef.map(result)(assertResult(expectedMessage)))

  }

  private val deploySet          = DeploySet(tvc)
  private val callSetHeader      = Option(Map("pubkey" -> keyPair.public.asJson, "time" -> eventsTime.asJson, "expire" -> eventsExpire.asJson))
  private val constructorCallSet = CallSet("constructor", callSetHeader)
  private val externalSigner     = Signer.fromExternal(keyPair.public)
  private val keyPairSigner      = Signer.fromKeypair(keyPair)

  it should "encode_message, attach_signature" in {
    // Create unsigned deployment message
    val unsignedF = local { implicit ctx =>
      call(Request.EncodeMessage(abi, None, Option(deploySet), Option(constructorCallSet), externalSigner))
    }
    assertExpression(unsignedF) { r: Result.EncodeMessage =>
      r.data_to_sign == Option("KCGM36iTYuCYynk+Jnemis+mcwi3RFCke95i7l96s4Q=") &&
      r.message == "te6ccgECFwEAA2gAAqeIAAt9aqvShfTon7Lei1PVOhUEkEEZQkhDKPgNyzeTL6YSEZTHxAj/Hd67jWQF7peccWoU/dbMCBJBB6YdPCVZcJlJkAAAF0ZyXLg19VzGRotV8/gGAQEBwAICA88gBQMBAd4EAAPQIABB2mPiBH+O713GsgL3S844tQp+62YECSCD0w6eEqy4TKTMAib/APSkICLAAZL0oOGK7VNYMPShCQcBCvSkIPShCAAAAgEgDAoByP9/Ie1E0CDXScIBjhDT/9M/0wDRf/hh+Gb4Y/hijhj0BXABgED0DvK91wv/+GJw+GNw+GZ/+GHi0wABjh2BAgDXGCD5AQHTAAGU0/8DAZMC+ELiIPhl+RDyqJXTAAHyeuLTPwELAGqOHvhDIbkgnzAg+COBA+iogggbd0Cgud6S+GPggDTyNNjTHwH4I7zyudMfAfAB+EdukvI83gIBIBINAgEgDw4AvbqLVfP/hBbo417UTQINdJwgGOENP/0z/TANF/+GH4Zvhj+GKOGPQFcAGAQPQO8r3XC//4YnD4Y3D4Zn/4YeLe+Ebyc3H4ZtH4APhCyMv/+EPPCz/4Rs8LAMntVH/4Z4AgEgERAA5biABrW/CC3Rwn2omhp/+mf6YBov/ww/DN8Mfwxb30gyupo6H0gb+j8IpA3SRg4b3whXXlwMnwAZGT9ghBkZ8KEZ0aCBAfQAAAAAAAAAAAAAAAAACBni2TAgEB9gBh8IWRl//wh54Wf/CNnhYBk9qo//DPAAxbmTwqLfCC3Rwn2omhp/+mf6YBov/ww/DN8Mfwxb2uG/8rqaOhp/+/o/ABkRe4AAAAAAAAAAAAAAAAIZ4tnwOfI48sYvRDnhf/kuP2AGHwhZGX//CHnhZ/8I2eFgGT2qj/8M8AIBSBYTAQm4t8WCUBQB/PhBbo4T7UTQ0//TP9MA0X/4Yfhm+GP4Yt7XDf+V1NHQ0//f0fgAyIvcAAAAAAAAAAAAAAAAEM8Wz4HPkceWMXohzwv/yXH7AMiL3AAAAAAAAAAAAAAAABDPFs+Bz5JW+LBKIc8L/8lx+wAw+ELIy//4Q88LP/hGzwsAye1UfxUABPhnAHLccCLQ1gIx0gAw3CHHAJLyO+Ah1w0fkvI84VMRkvI74cEEIoIQ/////byxkvI84AHwAfhHbpLyPN4="
    }
    val unsigned = ef.unsafeGet(unsignedF)

    // Create detached signature
    val signatureF = local { implicit ctx =>
      call(Crypto.Request.Sign(unsigned.data_to_sign.get, keyPair))
    }
    assertExpression(signatureF)(
      _.signature == Option("6272357bccb601db2b821cb0f5f564ab519212d242cf31961fe9a3c50a30b236012618296b4f769355c0e9567cd25b366f3c037435c498c82e5305622adbc70e")
    )
    val signature = ef.unsafeGet(signatureF)

    // Attach signature to unsigned message
    val attachedF = local { implicit ctx =>
      call(Request.AttachSignature(abi, keyPair.public, unsigned.message, signature.signature.get))
    }
    assertExpression(attachedF)(_.message == expectedSignedMessage)

    // Create initially signed message
    val signedF = local { implicit ctx =>
      call(Request.EncodeMessage(abi, None, Option(deploySet), Option(constructorCallSet), keyPairSigner))
    }
    assertExpression(signedF)(_.message == expectedSignedMessage)
  }

  it should "encode_message and run it" in {
    // Create run unsigned message
    val address         = "0:05beb555e942fa744fd96f45a9ea9d0a8248208ca12421947c06e59bc997d309"
    val retValueCallSet = CallSet("returnValue", callSetHeader, Option(Map("id" -> "0".asJson)))
    val runUnsignedF = local { implicit ctx =>
      call(Request.EncodeMessage(abi, Option(address), None, Option(retValueCallSet), externalSigner))
    }
    assertExpression(runUnsignedF) { r: Result.EncodeMessage =>
      r.data_to_sign == Option("i4Hs3PB12QA9UBFbOIpkG3JerHHqjm4LgvF4MA7TDsY=") &&
      r.message == "te6ccgEBAgEAeAABpYgAC31qq9KF9Oifst6LU9U6FQSQQRlCSEMo+A3LN5MvphIFMfECP8d3ruNZAXul5xxahT91swIEkEHph08JVlwmUmQAAAXRnJcuDX1XMZBW+LBKAQBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    }
    val runUnsigned = ef.unsafeGet(runUnsignedF)

    // Create detached signature
    val detachedSignatureF = local { implicit ctx =>
      call(Crypto.Request.Sign(runUnsigned.data_to_sign.get, keyPair))
    }
    assertExpression(detachedSignatureF)(
      _.signature == Option("5bbfb7f184f2cb5f019400b9cd497eeaa41f3d5885619e9f7d4fab8dd695f4b3a02159a1422996c1dd7d1be67898bc79c6adba6c65a18101ac5f0a2a2bb8910b")
    )
    val detachedSignature = ef.unsafeGet(detachedSignatureF)

    // Attach signature
    val attachedSignatureF = local { implicit ctx =>
      call(Request.AttachSignature(abi, keyPair.public, runUnsigned.message, detachedSignature.signature.get))
    }
    assertExpression(attachedSignatureF)(_.message == encodedMessage)
  }

  val encodeAccountExpectedId = "05beb555e942fa744fd96f45a9ea9d0a8248208ca12421947c06e59bc997d309"

  it should "encode_account from encoded deploy message" in {
    val encodedDeployMessage =
      "te6ccgECFwEAA2gAAqeIAAt9aqvShfTon7Lei1PVOhUEkEEZQkhDKPgNyzeTL6YSEZTHxAj/Hd67jWQF7peccWoU/dbMCBJBB6YdPCVZcJlJkAAAF0ZyXLg19VzGRotV8/gGAQEBwAICA88gBQMBAd4EAAPQIABB2mPiBH+O713GsgL3S844tQp+62YECSCD0w6eEqy4TKTMAib/APSkICLAAZL0oOGK7VNYMPShCQcBCvSkIPShCAAAAgEgDAoByP9/Ie1E0CDXScIBjhDT/9M/0wDRf/hh+Gb4Y/hijhj0BXABgED0DvK91wv/+GJw+GNw+GZ/+GHi0wABjh2BAgDXGCD5AQHTAAGU0/8DAZMC+ELiIPhl+RDyqJXTAAHyeuLTPwELAGqOHvhDIbkgnzAg+COBA+iogggbd0Cgud6S+GPggDTyNNjTHwH4I7zyudMfAfAB+EdukvI83gIBIBINAgEgDw4AvbqLVfP/hBbo417UTQINdJwgGOENP/0z/TANF/+GH4Zvhj+GKOGPQFcAGAQPQO8r3XC//4YnD4Y3D4Zn/4YeLe+Ebyc3H4ZtH4APhCyMv/+EPPCz/4Rs8LAMntVH/4Z4AgEgERAA5biABrW/CC3Rwn2omhp/+mf6YBov/ww/DN8Mfwxb30gyupo6H0gb+j8IpA3SRg4b3whXXlwMnwAZGT9ghBkZ8KEZ0aCBAfQAAAAAAAAAAAAAAAAACBni2TAgEB9gBh8IWRl//wh54Wf/CNnhYBk9qo//DPAAxbmTwqLfCC3Rwn2omhp/+mf6YBov/ww/DN8Mfwxb2uG/8rqaOhp/+/o/ABkRe4AAAAAAAAAAAAAAAAIZ4tnwOfI48sYvRDnhf/kuP2AGHwhZGX//CHnhZ/8I2eFgGT2qj/8M8AIBSBYTAQm4t8WCUBQB/PhBbo4T7UTQ0//TP9MA0X/4Yfhm+GP4Yt7XDf+V1NHQ0//f0fgAyIvcAAAAAAAAAAAAAAAAEM8Wz4HPkceWMXohzwv/yXH7AMiL3AAAAAAAAAAAAAAAABDPFs+Bz5JW+LBKIc8L/8lx+wAw+ELIy//4Q88LP/hGzwsAye1UfxUABPhnAHLccCLQ1gIx0gAw3CHHAJLyO+Ah1w0fkvI84VMRkvI74cEEIoIQ/////byxkvI84AHwAfhHbpLyPN4="
    val messageSource   = MessageSource.fromEncoded(encodedDeployMessage, Option(abi))
    val stateInitSource = StateInitSource.fromMessage(messageSource)

    val encodedF = local { implicit ctx =>
      call(Request.EncodeAccount(stateInitSource, None, None, None))
    }
    assertExpression(encodedF)(_.id == encodeAccountExpectedId)
  }

  it should "encode_account from encoding params" in {
    val messageSource   = MessageSource.fromEncodingParams(Request.EncodeMessage(abi, None, Option(deploySet), Option(constructorCallSet), keyPairSigner))
    val stateInitSource = StateInitSource.fromMessage(messageSource)

    val encodedF = local { implicit ctx =>
      call(Request.EncodeAccount(stateInitSource, None, None, None))
    }
    assertExpression(encodedF)(_.id == encodeAccountExpectedId)
  }

  it should "encode_account from tvc" in {
    val stateInitSource = StateInitSource.fromTvc(tvc, None, None)

    val encodedF = local { implicit ctx =>
      call(Request.EncodeAccount(stateInitSource, None, None, None))
    }
    assertExpression(encodedF)(_.id != encodeAccountExpectedId)
  }

  it should "encode_account from tvc and key" in {
    val stateInitSource = StateInitSource.fromTvc(tvc, Option(keyPair.public), None)

    val encodedF = local { implicit ctx =>
      call(Request.EncodeAccount(stateInitSource, None, None, None))
    }
    assertExpression(encodedF)(_.id == encodeAccountExpectedId)
  }

  it should "not encode_account" in {
    val messageSource   = MessageSource.fromEncodingParams(Request.EncodeMessage(abi, None, Option(deploySet), Option(constructorCallSet), externalSigner))
    val stateInitSource = StateInitSource.fromMessage(messageSource)

    val encodedF = local { implicit ctx =>
      call(Request.EncodeAccount(stateInitSource, None, None, None))
    }
    assertSdkError(encodedF)("Function `process_message` must not be used with external message signing.")
  }

  it should "encode_internal_message" in {
    val expectedBoc1 = Option("""te6ccgECFwEAA0UAAmFiAEJUqIWBPAI4qljcJbbqIuCaTsaha60XdnA9uVa3J1CbAAAAAAAAAAAAAAAAAAIyBgEBAcACAgPPIAUDAQHeBAAD0CAAQdgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABAIm/wD0pCAiwAGS9KDhiu1TWDD0oQkHAQr0pCD0oQgAAAIBIAwKAcj/fyHtRNAg10nCAY4Q0//TP9MA0X/4Yfhm+GP4Yo4Y9AVwAYBA9A7yvdcL//hicPhjcPhmf/hh4tMAAY4dgQIA1xgg+QEB0wABlNP/AwGTAvhC4iD4ZfkQ8qiV0wAB8nri0z8BCwBqjh74QyG5IJ8wIPgjgQPoqIIIG3dAoLnekvhj4IA08jTY0x8B+CO88rnTHwHwAfhHbpLyPN4CASASDQIBIA8OAL26i1Xz/4QW6ONe1E0CDXScIBjhDT/9M/0wDRf/hh+Gb4Y/hijhj0BXABgED0DvK91wv/+GJw+GNw+GZ/+GHi3vhG8nNx+GbR+AD4QsjL//hDzws/+EbPCwDJ7VR/+GeAIBIBEQAOW4gAa1vwgt0cJ9qJoaf/pn+mAaL/8MPwzfDH8MW99IMrqaOh9IG/o/CKQN0kYOG98IV15cDJ8AGRk/YIQZGfChGdGggQH0AAAAAAAAAAAAAAAAAAgZ4tkwIBAfYAYfCFkZf/8IeeFn/wjZ4WAZPaqP/wzwAMW5k8Ki3wgt0cJ9qJoaf/pn+mAaL/8MPwzfDH8MW9rhv/K6mjoaf/v6PwAZEXuAAAAAAAAAAAAAAAACGeLZ8DnyOPLGL0Q54X/5Lj9gBh8IWRl//wh54Wf/CNnhYBk9qo//DPACAUgWEwEJuLfFglAUAfz4QW6OE+1E0NP/0z/TANF/+GH4Zvhj+GLe1w3/ldTR0NP/39H4AMiL3AAAAAAAAAAAAAAAABDPFs+Bz5HHljF6Ic8L/8lx+wDIi9wAAAAAAAAAAAAAAAAQzxbPgc+SVviwSiHPC//JcfsAMPhCyMv/+EPPCz/4Rs8LAMntVH8VAAT4ZwBy3HAi0NYCMdIAMNwhxwCS8jvgIdcNH5LyPOFTEZLyO+HBBCKCEP////28sZLyPOAB8AH4R26S8jze""".stripMargin)
    testEncodeInternalMessageDeploy(abi, tvc, None, expectedBoc1, "9170a240d27f988c8d47a1a94d6630f1b67da11da365202c070c9fb4d938634a")
    val callSet = Option(CallSet("constructor"))
    val expectedBoc2 = Option("""te6ccgECFwEAA0kAAmliAEJUqIWBPAI4qljcJbbqIuCaTsaha60XdnA9uVa3J1CbAAAAAAAAAAAAAAAAAAIxotV8/gYBAQHAAgIDzyAFAwEB3gQAA9AgAEHYAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAQCJv8A9KQgIsABkvSg4YrtU1gw9KEJBwEK9KQg9KEIAAACASAMCgHI/38h7UTQINdJwgGOENP/0z/TANF/+GH4Zvhj+GKOGPQFcAGAQPQO8r3XC//4YnD4Y3D4Zn/4YeLTAAGOHYECANcYIPkBAdMAAZTT/wMBkwL4QuIg+GX5EPKoldMAAfJ64tM/AQsAao4e+EMhuSCfMCD4I4ED6KiCCBt3QKC53pL4Y+CANPI02NMfAfgjvPK50x8B8AH4R26S8jzeAgEgEg0CASAPDgC9uotV8/+EFujjXtRNAg10nCAY4Q0//TP9MA0X/4Yfhm+GP4Yo4Y9AVwAYBA9A7yvdcL//hicPhjcPhmf/hh4t74RvJzcfhm0fgA+ELIy//4Q88LP/hGzwsAye1Uf/hngCASAREADluIAGtb8ILdHCfaiaGn/6Z/pgGi//DD8M3wx/DFvfSDK6mjofSBv6PwikDdJGDhvfCFdeXAyfABkZP2CEGRnwoRnRoIEB9AAAAAAAAAAAAAAAAAAIGeLZMCAQH2AGHwhZGX//CHnhZ/8I2eFgGT2qj/8M8ADFuZPCot8ILdHCfaiaGn/6Z/pgGi//DD8M3wx/DFva4b/yupo6Gn/7+j8AGRF7gAAAAAAAAAAAAAAAAhni2fA58jjyxi9EOeF/+S4/YAYfCFkZf/8IeeFn/wjZ4WAZPaqP/wzwAgFIFhMBCbi3xYJQFAH8+EFujhPtRNDT/9M/0wDRf/hh+Gb4Y/hi3tcN/5XU0dDT/9/R+ADIi9wAAAAAAAAAAAAAAAAQzxbPgc+Rx5YxeiHPC//JcfsAyIvcAAAAAAAAAAAAAAAAEM8Wz4HPklb4sEohzwv/yXH7ADD4QsjL//hDzws/+EbPCwDJ7VR/FQAE+GcActxwItDWAjHSADDcIccAkvI74CHXDR+S8jzhUxGS8jvhwQQighD////9vLGS8jzgAfAB+EdukvI83g==""".stripMargin)
    testEncodeInternalMessageDeploy(abi, tvc, callSet, expectedBoc2, "d9c17520aa562ce7f0d754eddf3ebe43f5608cd1f43f316089f099305b6d616e")
  }



  private def testEncodeInternalMessageDeploy(abi: AbiJson, tvc: String, callSet: Option[CallSet], expectedBoc: Option[String], expectedMessageId: String) = {
    val deploySet = Option(DeploySet(tvc))
    val resultF = local { implicit ctx =>
        call(Request.EncodeInternalMessage(Option(abi), None, deploySet, callSet, "0", None, None))
    }
    assertExpression(resultF) { r: Result.EncodeInternalMessage =>
      r.address == "0:84a9510b0278047154b1b84b6dd445c1349d8d42d75a2eece07b72ad6e4ea136" &&
        r.message_id == expectedMessageId && expectedBoc.forall(_ == r.message)
    }
  }

}
