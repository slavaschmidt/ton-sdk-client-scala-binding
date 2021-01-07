package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding.Api.{DebotCallback, DebotExecute, DebotHandle, DebotSdkCall, SdkCall}
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

  object ResultOfAppDebotBrowser {
    case object InvokeDebot                          extends BaseAppCallback
    final case class Input(value: String)            extends BaseAppCallback
    final case class GetSigningBox(signing_box: Int) extends BaseAppCallback
  }

  final case class Address(address: String)
  final case class AppDebotBrowser(address: String)
  final case class DebotAction(description: String, name: String, action_type: Int, to: Int, attributes: String, misc: String) extends BaseAppCallback

  object Request {
    final case class Start(address: Address)
    final case class Fetch(address: Address)
    final case class Execute[C](debot_handle: DebotHandle, action: DebotAction, callback: DebotCallback[C]) extends DebotExecute[C]
    final case class Remove(debot_handle: DebotHandle)
  }

  object Result {
    final case class RegisteredDebot(debot_handle: DebotHandle)
  }

  import io.circe.generic.auto._

  object ParamsOfAppDebotBrowser {
    case object GetSigningBox                                             extends BaseAppCallback
    final case class Log(msg: String)                                     extends BaseAppCallback
    final case class Switch(context_id: Int)                              extends BaseAppCallback
    final case class SwitchCompleted(context_id: Int)                     extends BaseAppCallback
    final case class Input(prompt: String)                                extends BaseAppCallback
    final case class ShowAction(action: DebotAction)                      extends BaseAppCallback
    final case class InvokeDebot(debot_addr: String, action: DebotAction) extends BaseAppCallback

    def fromMap(map: Json, tpe: String) = tpe match {
      case "GetSigningBox"   => Right(GetSigningBox)
      case "Log"             => map.as[Log]
      case "Switch"          => map.as[Switch]
      case "SwitchCompleted" => map.as[SwitchCompleted]
      case "Input"           => map.as[Input]
      case "ShowAction"      => map.as[ShowAction]
      case "InvokeDebot"     => map.as[InvokeDebot]
    }

    def apply(param: Json): Option[BaseAppCallback] = {
      val tpe = param.hcursor.get[String]("type").toOption
      fromMap(param, tpe.get).toOption
    }
  }

  implicit val start  = new SdkCall[Request.Start, Result.RegisteredDebot] { override val function: String = s"$module.start"  }
  implicit val fetch  = new SdkCall[Request.Fetch, Result.RegisteredDebot] { override val function: String = s"$module.fetch"  }
  implicit val remove = new SdkCall[Request.Remove, Unit]                  { override val function: String = s"$module.remove" }

  import ton.sdk.client.binding.Decoders.encodeDebotExecute
  implicit def execute[T] = new DebotSdkCall[Request.Execute[T], Unit, T] { override val function: String = s"$module.execute" }

}
