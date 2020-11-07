package ton.sdk.client.modules

import io.circe.literal._
import io.circe.syntax._
import org.scalatest.flatspec._
import ton.sdk.client.binding._
import ton.sdk.client.modules.Abi._
import ton.sdk.client.modules.Context._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class SyncAbiSpec extends AbiSpec[Try] {
  implicit override val fe: Context.Effect[Try] = tryEffect
}

class AsyncAbSpec extends AbiSpec[Future] {
  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val fe: Context.Effect[Future]         = futureEffect
}

abstract class AbiSpec[T[_]] extends AsyncFlatSpec with SdkAssertions[T] {

  behavior of "Abi"

  implicit val fe: Effect[T]

  val keyPair      = KeyPair(public = "4c7c408ff1ddebb8d6405ee979c716a14fdd6cc08124107a61d3c25597099499", secret = "cc8929d635719612a9478b9cd17675a39cfad52d8959e8a177389b8c0b9122a7")
  val abi          = Abi.fromFile(getClass.getClassLoader.getResource("Events.abi.json").getFile).toOption.get
  val tvcSrc       = getClass.getClassLoader.getResourceAsStream("Events.tvc").readAllBytes()
  val tvc          = base64(tvcSrc)
  val eventsTime   = 1599458364291L
  val eventsExpire = 1599458404

  println(abi)

  val encodedMessage =
    "te6ccgEBAwEAvAABRYgAC31qq9KF9Oifst6LU9U6FQSQQRlCSEMo+A3LN5MvphIMAQHhrd/b+MJ5Za+AygBc5qS/dVIPnqxCsM9PvqfVxutK+lnQEKzQoRTLYO6+jfM8TF4841bdNjLQwIDWL4UVFdxIhdMfECP8d3ruNZAXul5xxahT91swIEkEHph08JVlwmUmQAAAXRnJcuDX1XMZBW+LBKACAEAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=="

  val expectedSignedMessage =
    "te6ccgECGAEAA6wAA0eIAAt9aqvShfTon7Lei1PVOhUEkEEZQkhDKPgNyzeTL6YSEbAHAgEA4bE5Gr3mWwDtlcEOWHr6slWoyQlpIWeYyw/00eKFGFkbAJMMFLWnu0mq4HSrPmktmzeeAboa4kxkFymCsRVt44dTHxAj/Hd67jWQF7peccWoU/dbMCBJBB6YdPCVZcJlJkAAAF0ZyXLg19VzGRotV8/gAQHAAwIDzyAGBAEB3gUAA9AgAEHaY+IEf47vXcayAvdLzji1Cn7rZgQJIIPTDp4SrLhMpMwCJv8A9KQgIsABkvSg4YrtU1gw9KEKCAEK9KQg9KEJAAACASANCwHI/38h7UTQINdJwgGOENP/0z/TANF/+GH4Zvhj+GKOGPQFcAGAQPQO8r3XC//4YnD4Y3D4Zn/4YeLTAAGOHYECANcYIPkBAdMAAZTT/wMBkwL4QuIg+GX5EPKoldMAAfJ64tM/AQwAao4e+EMhuSCfMCD4I4ED6KiCCBt3QKC53pL4Y+CANPI02NMfAfgjvPK50x8B8AH4R26S8jzeAgEgEw4CASAQDwC9uotV8/+EFujjXtRNAg10nCAY4Q0//TP9MA0X/4Yfhm+GP4Yo4Y9AVwAYBA9A7yvdcL//hicPhjcPhmf/hh4t74RvJzcfhm0fgA+ELIy//4Q88LP/hGzwsAye1Uf/hngCASASEQDluIAGtb8ILdHCfaiaGn/6Z/pgGi//DD8M3wx/DFvfSDK6mjofSBv6PwikDdJGDhvfCFdeXAyfABkZP2CEGRnwoRnRoIEB9AAAAAAAAAAAAAAAAAAIGeLZMCAQH2AGHwhZGX//CHnhZ/8I2eFgGT2qj/8M8ADFuZPCot8ILdHCfaiaGn/6Z/pgGi//DD8M3wx/DFva4b/yupo6Gn/7+j8AGRF7gAAAAAAAAAAAAAAAAhni2fA58jjyxi9EOeF/+S4/YAYfCFkZf/8IeeFn/wjZ4WAZPaqP/wzwAgFIFxQBCbi3xYJQFQH8+EFujhPtRNDT/9M/0wDRf/hh+Gb4Y/hi3tcN/5XU0dDT/9/R+ADIi9wAAAAAAAAAAAAAAAAQzxbPgc+Rx5YxeiHPC//JcfsAyIvcAAAAAAAAAAAAAAAAEM8Wz4HPklb4sEohzwv/yXH7ADD4QsjL//hDzws/+EbPCwDJ7VR/FgAE+GcActxwItDWAjHSADDcIccAkvI74CHXDR+S8jzhUxGS8jvhwQQighD////9vLGS8jzgAfAB+EdukvI83g=="

  it should "decode_message Input" in {
    val result = local { implicit ctx =>
      call(Request.DecodeMessage(abi, encodedMessage))
    }
    val expectedValue   = json"""{"id": "0x0000000000000000000000000000000000000000000000000000000000000000"}"""
    val header          = FunctionHeader(Option(1599458404), Option(1599458364291L), Option("4c7c408ff1ddebb8d6405ee979c716a14fdd6cc08124107a61d3c25597099499"))
    val expectedMessage = Result.DecodedMessageBody(MessageBodyType.input, "returnValue", Option(expectedValue), Option(header))
    fe.unsafeGet(fe.map(result)(assertResult(expectedMessage)))
  }

  it should "decode_message Event" in {
    val message = "te6ccgEBAQEAVQAApeACvg5/pmQpY4m61HmJ0ne+zjHJu3MNG8rJxUDLbHKBu/AAAAAAAAAMJL6z6ro48sYvAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABA"
    val result = local { implicit ctx =>
      call(Request.DecodeMessage(abi, message))
    }

    val expectedValue   = json"""{"id": "0x0000000000000000000000000000000000000000000000000000000000000000"}"""
    val expectedMessage = Result.DecodedMessageBody(MessageBodyType.event, "EventThrown", Option(expectedValue), None)
    fe.unsafeGet(fe.map(result)(assertResult(expectedMessage)))
  }

  it should "decode_message Output" in {
    val message = "te6ccgEBAQEAVQAApeACvg5/pmQpY4m61HmJ0ne+zjHJu3MNG8rJxUDLbHKBu/AAAAAAAAAMKr6z6rxK3xYJAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAABA"
    val result = local { implicit ctx =>
      call(Request.DecodeMessage(abi, message))
    }

    val expectedValue   = json"""{"value0": "0x0000000000000000000000000000000000000000000000000000000000000000"}"""
    val expectedMessage = Result.DecodedMessageBody(MessageBodyType.output, "returnValue", Option(expectedValue), None)
    fe.unsafeGet(fe.map(result)(assertResult(expectedMessage)))
  }

  it should "not decode_message" in {
    val result = local { implicit ctx =>
      call(Request.DecodeMessage(abi, "Oh Weh"))
    }
    assertSdkError(result)("Invalid base64 string: Invalid byte 32, offset 2.\r\nbase64: [Oh Weh]")
  }

  it should "decode_message_body" in {
    val result = local { implicit ctx =>
      fe.flatMap(call(Boc.Request.ParseMessage(encodedMessage))) { parsed =>
        call(Request.DecodeMessage(abi, parsed.parsed.body))
      }
    }

    val expectedValue   = json"""{"id": "0x0000000000000000000000000000000000000000000000000000000000000000"}"""
    val header          = FunctionHeader(Option(1599458404), Option(1599458364291L), Option("4c7c408ff1ddebb8d6405ee979c716a14fdd6cc08124107a61d3c25597099499"))
    val expectedMessage = Result.DecodedMessageBody(MessageBodyType.input, "returnValue", Option(expectedValue), Option(header))
    fe.unsafeGet(fe.map(result)(assertResult(expectedMessage)))
  }

  val deploySet          = DeploySet(tvc)
  val callSetHeader      = Option(Map("pubkey" -> keyPair.public.asJson, "time" -> eventsTime.asJson, "expire" -> eventsExpire.asJson))
  val constructorCallSet = CallSet("constructor", callSetHeader)
  val externalSigner     = ExternalSigner(keyPair.public)

  it should "encode_message, attach_signature" in {
    // Create unsigned deployment message
    val unsignedF = local { implicit ctx =>
      call(Request.EncodeMessage(abi, None, Option(deploySet), Option(constructorCallSet), externalSigner, None))
    }
    assertExpression(unsignedF) { r: Result.EncodeMessage =>
      r.data_to_sign == Option("KCGM36iTYuCYynk+Jnemis+mcwi3RFCke95i7l96s4Q=") &&
      r.message == "te6ccgECFwEAA2gAAqeIAAt9aqvShfTon7Lei1PVOhUEkEEZQkhDKPgNyzeTL6YSEZTHxAj/Hd67jWQF7peccWoU/dbMCBJBB6YdPCVZcJlJkAAAF0ZyXLg19VzGRotV8/gGAQEBwAICA88gBQMBAd4EAAPQIABB2mPiBH+O713GsgL3S844tQp+62YECSCD0w6eEqy4TKTMAib/APSkICLAAZL0oOGK7VNYMPShCQcBCvSkIPShCAAAAgEgDAoByP9/Ie1E0CDXScIBjhDT/9M/0wDRf/hh+Gb4Y/hijhj0BXABgED0DvK91wv/+GJw+GNw+GZ/+GHi0wABjh2BAgDXGCD5AQHTAAGU0/8DAZMC+ELiIPhl+RDyqJXTAAHyeuLTPwELAGqOHvhDIbkgnzAg+COBA+iogggbd0Cgud6S+GPggDTyNNjTHwH4I7zyudMfAfAB+EdukvI83gIBIBINAgEgDw4AvbqLVfP/hBbo417UTQINdJwgGOENP/0z/TANF/+GH4Zvhj+GKOGPQFcAGAQPQO8r3XC//4YnD4Y3D4Zn/4YeLe+Ebyc3H4ZtH4APhCyMv/+EPPCz/4Rs8LAMntVH/4Z4AgEgERAA5biABrW/CC3Rwn2omhp/+mf6YBov/ww/DN8Mfwxb30gyupo6H0gb+j8IpA3SRg4b3whXXlwMnwAZGT9ghBkZ8KEZ0aCBAfQAAAAAAAAAAAAAAAAACBni2TAgEB9gBh8IWRl//wh54Wf/CNnhYBk9qo//DPAAxbmTwqLfCC3Rwn2omhp/+mf6YBov/ww/DN8Mfwxb2uG/8rqaOhp/+/o/ABkRe4AAAAAAAAAAAAAAAAIZ4tnwOfI48sYvRDnhf/kuP2AGHwhZGX//CHnhZ/8I2eFgGT2qj/8M8AIBSBYTAQm4t8WCUBQB/PhBbo4T7UTQ0//TP9MA0X/4Yfhm+GP4Yt7XDf+V1NHQ0//f0fgAyIvcAAAAAAAAAAAAAAAAEM8Wz4HPkceWMXohzwv/yXH7AMiL3AAAAAAAAAAAAAAAABDPFs+Bz5JW+LBKIc8L/8lx+wAw+ELIy//4Q88LP/hGzwsAye1UfxUABPhnAHLccCLQ1gIx0gAw3CHHAJLyO+Ah1w0fkvI84VMRkvI74cEEIoIQ/////byxkvI84AHwAfhHbpLyPN4="
    }
    val unsigned = fe.unsafeGet(unsignedF)

    // Create detached signature
    val signatureF = local { implicit ctx =>
      call(Crypto.Request.Sign(unsigned.data_to_sign.get, keyPair))
    }
    assertExpression(signatureF)(
      _.signature == Option("6272357bccb601db2b821cb0f5f564ab519212d242cf31961fe9a3c50a30b236012618296b4f769355c0e9567cd25b366f3c037435c498c82e5305622adbc70e")
    )
    val signature = fe.unsafeGet(signatureF)

    // Attach signature to unsigned message
    val attachedF = local { implicit ctx =>
      call(Request.AttachSignature(abi, keyPair.public, unsigned.message, signature.signature.get))
    }
    assertExpression(attachedF)(_.message == expectedSignedMessage)

    // Create initially signed message
    val keyPairSigner = KeysSigner(keyPair)
    val signedF = local { implicit ctx =>
      call(Request.EncodeMessage(abi, None, Option(deploySet), Option(constructorCallSet), keyPairSigner, None))
    }
    assertExpression(signedF)(_.message == expectedSignedMessage)

    // Create run unsigned message
    val retValueCallSet = CallSet("returnValue", callSetHeader, Option(Map("id" -> 0.asJson)))
    val runUnsignedF = local { implicit ctx =>
      call(Request.EncodeMessage(abi, None, Option(deploySet), Option(retValueCallSet), externalSigner, None))
    }
    assertExpression(runUnsignedF) { r: Result.EncodeMessage =>
      r.data_to_sign == Option("i4Hs3PB12QA9UBFbOIpkG3JerHHqjm4LgvF4MA7TDsY=") &&
      r.message == "te6ccgEBAgEAeAABpYgAC31qq9KF9Oifst6LU9U6FQSQQRlCSEMo+A3LN5MvphIFMfECP8d3ruNZAXul5xxahT91swIEkEHph08JVlwmUmQAAAXRnJcuDX1XMZBW+LBKAQBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
    }
    val runUnsigned = fe.unsafeGet(runUnsignedF)

    // Create detached signature
    val detachedSignatureF = local { implicit ctx =>
      call(Crypto.Request.Sign(runUnsigned.data_to_sign.get, keyPair))
    }
    assertExpression(detachedSignatureF)(
      _.signature == Option("5bbfb7f184f2cb5f019400b9cd497eeaa41f3d5885619e9f7d4fab8dd695f4b3a02159a1422996c1dd7d1be67898bc79c6adba6c65a18101ac5f0a2a2bb8910b")
    )
    val detachedSignature = fe.unsafeGet(detachedSignatureF)

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
    val messageSource   = EncodedMessageSource(encodedDeployMessage, Option(abi))
    val stateInitSource = MessageStateInitSource(messageSource)

    val encodedF = local { implicit ctx =>
      call(Request.EncodeAccount(stateInitSource, None, None, None))
    }
    assertExpression(encodedF)(_.id == encodeAccountExpectedId)
  }

  // TODO the message source is probably wrong here
  it should "encode_account from encoding params" in {
    val messageSource   = EncodingParamsMessageSource(Request.EncodeMessage(abi, None, Option(deploySet), Option(constructorCallSet), externalSigner, None))
    val stateInitSource = MessageStateInitSource(messageSource)

    val encodedF = local { implicit ctx =>
      call(Request.EncodeAccount(stateInitSource, None, None, None))
    }
    assertExpression(encodedF)(_.id == encodeAccountExpectedId)
  }

  it should "encode_account from tvc" in {
    val stateInitSource = TvcStateInitSource(tvc, None, None)

    val encodedF = local { implicit ctx =>
      call(Request.EncodeAccount(stateInitSource, None, None, None))
    }
    assertExpression(encodedF)(_.id == encodeAccountExpectedId)
  }

  it should "encode_account from tvc and key" in {
    val stateInitSource = TvcStateInitSource(tvc, Option(keyPair.public), None)

    val encodedF = local { implicit ctx =>
      call(Request.EncodeAccount(stateInitSource, None, None, None))
    }
    assertExpression(encodedF)(_.id == encodeAccountExpectedId)
  }

  it should "not encode_account" in {
    val messageSource   = EncodingParamsMessageSource(Request.EncodeMessage(abi, None, Option(deploySet), Option(constructorCallSet), externalSigner, None))
    val stateInitSource = MessageStateInitSource(messageSource)

    val encodedF = local { implicit ctx =>
      call(Request.EncodeAccount(stateInitSource, None, None, None))
    }
    assertExpression(encodedF)(_.id == encodeAccountExpectedId)
  }
}
