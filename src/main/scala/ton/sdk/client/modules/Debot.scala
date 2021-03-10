package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding.Api.{DebotCall, DebotHandle, SdkCall}
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

  //final case class Address(address: String)
  final case class AppDebotBrowser(address: String)
  final case class DebotAction(description: String, name: String, action_type: Int, to: Int, attributes: String, misc: String) extends BaseAppCallback

  object Request {
    final case class Start(address: String)
    final case class Fetch(address: String)
    final case class Execute(debot_handle: DebotHandle, action: DebotAction)
    final case class Remove(debot_handle: DebotHandle)
    final case class Send(debot_handle: DebotHandle, source: String, func_id: Long, params: String)
  }

  object Result {
    final case class RegisteredDebot(debot_handle: DebotHandle)
  }

  import io.circe.generic.auto._

  object ParamsOfAppDebotBrowser {
    case object GetSigningBox                                             extends BaseAppCallback
    final case class Log(msg: String)                                     extends BaseAppCallback
    final case class Send(message: String)                                extends BaseAppCallback
    final case class Switch(context_id: Int)                              extends BaseAppCallback
    final case class SwitchCompleted(context_id: Option[Int])             extends BaseAppCallback
    final case class Input(prompt: String)                                extends BaseAppCallback
    final case class ShowAction(action: DebotAction)                      extends BaseAppCallback
    final case class InvokeDebot(debot_addr: String, action: DebotAction) extends BaseAppCallback

    def fromMap(map: Json, tpe: String) = tpe match {
      case "GetSigningBox"   => Right(GetSigningBox)
      case "Log"             => map.as[Log]
      case "Send"            => map.as[Send]
      case "Switch"          => map.as[Switch]
      case "SwitchCompleted" => map.as[SwitchCompleted]
      case "Input"           => map.as[Input]
      case "ShowAction"      => map.as[ShowAction]
      case "InvokeDebot"     => map.as[InvokeDebot]
    }

    def apply(param: Json): Option[BaseAppCallback] = {
      val tpe      = param.hcursor.get[String]("type").toOption
      val callback = fromMap(param, tpe.get).toOption
      callback
    }
  }

  implicit val remove  = new SdkCall[Request.Remove, Unit]                    { override val function: String = s"$module.remove"  }
  implicit val fetch   = new DebotCall[Request.Fetch, Result.RegisteredDebot] { override val function: String = s"$module.fetch"   }
  implicit val start   = new DebotCall[Request.Start, Result.RegisteredDebot] { override val function: String = s"$module.start"   }
  implicit val execute = new DebotCall[Request.Execute, Unit]                 { override val function: String = s"$module.execute" }
  implicit val send    = new DebotCall[Request.Send, Unit]                    { override val function: String = s"$module.send"    }

}
