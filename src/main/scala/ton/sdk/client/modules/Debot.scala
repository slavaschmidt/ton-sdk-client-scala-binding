package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding.Api.{SdkCall, StreamingSdkCall}
import ton.sdk.client.binding.BaseAppCallback

/**
  * Module debot
  *
  * UNSTABLE.
  *
  * Please refer to the [[https://github.com/tonlabs/TON-SDK/blob/master/docs/mod_debot.md SDK documentation]]
  * for the detailed description of individual functions and parameters
  *
  */
// scalafmt: { maxColumn = 300 }
object Debot {

  private val module = "debot"

  object DebotState {
    val ZERO    = 0
    val CURRENT = 253
    val PREV    = 254
    val EXIT    = 255
  }

  type DebotHandle = Int

  object ParamsOfAppDebotBrowser {
    case object GetSigningBox                                                        extends BaseAppCallback
    final case class Log(msg: String)                                                extends BaseAppCallback
    final case class Switch(context_id: Int)                                         extends BaseAppCallback
    final case class Input(prompt: String)                                           extends BaseAppCallback
    final case class ShowAction(action: String = "DebotAction")                      extends BaseAppCallback
    final case class InvokeDebot(debot_addr: String, action: String = "DebotAction") extends BaseAppCallback
    def apply(param: Map[String, Json]): BaseAppCallback = ??? // TODO
  }

  object ResultOfAppDebotBrowser {
    case object InvokeDebot                          extends BaseAppCallback
    final case class Input(value: String)            extends BaseAppCallback
    final case class GetSigningBox(signing_box: Int) extends BaseAppCallback
  }

  final case class Address(address: String)
  final case class AppDebotBrowser(address: String)
  final case class DebotAction(description: String, name: String, action_type: Int, to: Int, attributes: String, misc: String)

  object Request {
    final case class Start(address: Address)
    final case class Fetch(address: Address)
    final case class Execute(debot_handle: DebotHandle, action: DebotAction)
    final case class Remove(debot_handle: DebotHandle)
  }

  object Result {
    final case class RegisteredDebot(debot_handle: DebotHandle)
  }

  import io.circe.generic.auto._

  implicit val start   = new StreamingSdkCall[Request.Start, Result.RegisteredDebot, Json] { override val function: String = s"$module.start"   }
  implicit val fetch   = new StreamingSdkCall[Request.Fetch, Result.RegisteredDebot, Json] { override val function: String = s"$module.fetch"   }
  implicit val execute = new SdkCall[Request.Execute, Unit]                                { override val function: String = s"$module.execute" }
  implicit val remove  = new SdkCall[Request.Remove, Unit]                                 { override val function: String = s"$module.remove"  }

}
