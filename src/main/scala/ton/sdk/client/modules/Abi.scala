package ton.sdk.client.modules

import ton.sdk.client.modules.Api._

object Abi {

  private val prefix = "abi"

  object Request {
    case class AbiHandle(handle: BigDecimal)
    case class AbiContract(wtf: String)
    case class Abi(contract: AbiContract)
    case class Handle(handle: AbiHandle)
  }
  object Result {}
}
