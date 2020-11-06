package ton.sdk.client.modules

import ton.sdk.client.modules.Api._

// TODO this module is WIP
object Abi {

  private val prefix = "abi"

  // case class AbiHandle(handle: BigDecimal)
  // case class AbiContract(wtf: String)
  // case class Abi(contract: AbiContract)
  // case class Handle(handle: AbiHandle)
  case class Parameter(
    components: Option[Seq[Parameter]],
    name: String,
    `type`: String
  )
  case class Function(
    name: String,
    inputs: Seq[Parameter],
    outputs: Seq[Parameter]
  )
  case class Abi(
    `ABI version`: Double,
    header: List[String],
    functions: List[Function],
    data: Option[List[String]], // TODO empty array, need more examples
    events: List[Function]
  )

  object Request {
    case class DecodeMessage(abi: Abi, message: String)

  }
  object Result {
    case class DecodedMessage(body_type: String)
  }

  import io.circe.generic.auto._

  implicit val decodeMessage = new SdkCall[Request.DecodeMessage, String] {
    override val functionName: String = s"$prefix.decode_message"
  }
}
