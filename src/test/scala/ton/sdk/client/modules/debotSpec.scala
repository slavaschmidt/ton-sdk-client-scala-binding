package ton.sdk.client.modules

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import org.scalatest.Ignore
import org.scalatest.flatspec._
import ton.sdk.client.binding.Api.{DebotHandle, ResponseType, ResponseTypeAppNotify, ResponseTypeAppRequest}
import ton.sdk.client.binding.Context._
import ton.sdk.client.binding._
import ton.sdk.client.modules.Abi.AbiJson
import ton.sdk.client.modules.Debot.{DebotAction, DebotState, ParamsOfAppDebotBrowser, Request, ResultOfAppDebotBrowser}
import ton.sdk.client.modules.Processing.MessageEncodeParams

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}

case class Step(choice: Int, inputs: Seq[String], output: Seq[String], actions: Int, invokes: Seq[Step] = Nil)
case class State(handle: Option[DebotHandle], messages: Seq[String], actions: Seq[DebotAction], steps: Seq[Step], step: Option[Step], done: Boolean = false, switchStarted: Boolean = false) {
  lazy val messageLog = messages.map(m => s"[LOG]\t$m").mkString("\n")
  lazy val actionLog  = actions.map(m => s"[ACTION]\t$m").mkString("\n")

  def print(): Unit = println(messageLog + "\n" + actionLog)
}

@Ignore
class AsyncDebotSpec extends AsyncFlatSpec with SdkAssertions[Future] {
  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val ef: Context.Effect[Future]         = futureEffect
  implicit private val timeout                             = 1.minute

  val (debotAddr, targetAddr, keys) = Await.result(local { implicit ctx =>
    deploy()
  }, 1.minute)

  behavior of "Debot"

  it should "run debot with steps" in {
    val result = devNet { implicit ctx =>
      Future(debotRun(gotoSteps, "start", Nil))
    }
    assertValue(result)(null)
  }

  private val gotoSteps = Seq(
    Step(0, Nil, Seq("Test Goto Action"), 1),
    Step(0, Nil, Seq("Debot Tests"), 8),
    Step(7, Nil, Nil, 0)
  )

  private val printSteps = Seq(
    Step(1, Nil, Seq("Test Print Action", "test2: instant print", "test instant print"), 3),
    Step(0, Nil, Seq("test simple print"), 3),
    Step(1, Nil, Seq(s"integer=1,addr=${targetAddr.get},string=test_string_1"), 3),
    Step(2, Nil, Seq("Debot Tests"), 8),
    Step(7, Nil, Nil, 0)
  )

  private val runSteps = Seq(
    Step(2, Seq("-1:1111111111111111111111111111111111111111111111111111111111111111"), Seq("Test Run Action", "test1: instant run 1", "test2: instant run 2"), 3),
    Step(0, Nil, Nil, 3),
    Step(1, Nil, Seq("integer=2,addr=-1:1111111111111111111111111111111111111111111111111111111111111111,string=hello"), 3),
    Step(2, Nil, Seq("Debot Tests"), 8),
    Step(7, Nil, Nil, 0)
  )

  private val runMethodSteps = Seq(
    Step(3, Nil, Seq("Test Run Method Action"), 3),
    Step(0, Nil, Nil, 3),
    Step(1, Nil, Seq("data=64"), 3),
    Step(2, Nil, Seq("Debot Tests"), 8),
    Step(7, Nil, Nil, 0)
  )

  private val sendMessageSteps = Seq(
    Step(4, Nil, Seq("Test Send Msg Action"), 4),
    Step(0, Nil, Seq("Sending message {}", "Transaction succeeded."), 4),
    Step(1, Nil, Nil, 4),
    Step(2, Nil, Seq("data=100"), 4),
    Step(3, Nil, Seq("Debot Tests"), 8),
    Step(7, Nil, Nil, 0)
  )

  private val invokeSteps = Seq(
    Step(5, Seq(debotAddr.get), Seq("Test Invoke Debot Action", "enter debot address:"), 2),
    Step(0, Nil, Nil, 2, Seq(Step(0, Nil, Seq("Print test string", "Debot is invoked"), 0))),
    Step(1, Nil, Seq("Debot Tests"), 8),
    Step(7, Nil, Nil, 0)
  )

  implicit val debotStateCall: Api.DebotSdkCall[Request.Execute[State], Unit, State] = Debot.execute[State]

  def debotRun(steps: Seq[Step], startFn: String, actions: Seq[DebotAction])(implicit ctx: Context): State = {
    val state = State(None, Nil, actions, steps, None)

    val result = debotLoop(timeout, state, startFn)

    Await.result(call(Debot.Request.Remove(state.handle.get)), timeout)

    result
  }

  private def debotLoop(timeout: Duration, state: State, startFn: String)(implicit ctx: Context): State =
    state.steps match {
      case step :: tl if !state.done =>
        val action = state.actions(step.choice)
        println(s"[ACTION SELECTED]\t$action")
        val newState = state.copy(steps = tl, step = Option(step), messages = Nil)
        val callback = debotBrowser(Debot.Address(debotAddr.get), keys)(newState, startFn)
        Await.ready(call(Debot.Request.Execute(state.handle.get, action, callback)), timeout)
        assert(state.messages.size == step.output.size)
        state.print
        state
      case _ =>
        // No steps or done
        state
    }

  def debotBrowser(debotAddress: Debot.Address, keyPair: KeyPair)(state: State, startFn: String)(implicit ctx: Context) =
    (responseType: ResponseType, responseData: Json) =>
      // .get[Json]("response_data"), response_type
      responseType match {
        case ResponseTypeAppNotify =>
          handleAppNotify(state, responseData)
        case ResponseTypeAppRequest =>
          handleAppRequest(keyPair, state, timeout, responseData.as[Map[String, Json]].toOption.get)
        case other =>
          fail(s"Unexpected response data $other")
//          case Right(ResponseTypeResult) =>
//            state.copy(handle = Option(debot.debot_handle)) // data("debot_handle")
      }

  private def handleAppRequest(keyPair: KeyPair, state: State, timeout: Duration, data: Map[String, Json])(implicit ctx: Context): State = {
    val browserParams = ParamsOfAppDebotBrowser(data("request_data")).get
    val (result, newState) = browserParams match {
      case ParamsOfAppDebotBrowser.Input(_) =>
        (ResultOfAppDebotBrowser.Input(value = state.step.get.inputs.head).asJson, state)
      case ParamsOfAppDebotBrowser.GetSigningBox =>
        val box = Await.result(call(Crypto.Request.GetSigningBox(keyPair.public, keyPair.secret)), timeout)
        (ResultOfAppDebotBrowser.GetSigningBox(box.handle).asJson, state)
      case ParamsOfAppDebotBrowser.InvokeDebot(_, action) =>
        val steps    = state.step.get.invokes
        val newState = debotRun(steps, "fetch", Seq(action))
        (ResultOfAppDebotBrowser.InvokeDebot.asJson, newState)
    }
    val finalResult  = Client.AppRequest.Ok(result)
    val appRequestId = data("app_request_id").as[DebotHandle].toOption.get
    val _            = call(Client.Request.ResolveAppRequest(appRequestId, finalResult))
    newState
  }

  private def handleAppNotify(state: State, data: Json) = {
    ParamsOfAppDebotBrowser(data).get match {
      case ParamsOfAppDebotBrowser.Log(msg) =>
        state.copy(messages = state.messages :+ msg)
      case ParamsOfAppDebotBrowser.Switch(contextId) =>
        val done = contextId == DebotState.EXIT
        state.copy(actions = Nil, done = done, switchStarted = true)
      case ParamsOfAppDebotBrowser.SwitchCompleted(contextId) =>
        state.copy(switchStarted = false)
      case ParamsOfAppDebotBrowser.ShowAction(action) =>
        state.copy(actions = state.actions :+ action)
    }
  }

  private def deploy()(implicit ctx: Context): Future[(Option[String], Option[String], KeyPair)] = {

    val targetAbi = AbiJson.fromResource("DebotTarget.abi.json").toOption.get
    val debotAbi  = AbiJson.fromResource("Debot.abi.json").toOption.get
    val targetTvc = tvcFromResource("DebotTarget.tvc")
    val debotTvc  = tvcFromResource("DebotTarget.tvc")
    val callSet   = CallSet("constructor")
    val deploySet = DeploySet(targetTvc)

    for {
      keyPair <- call(Crypto.Request.GenerateRandomSignKeys)
      signer = Signer.fromKeypair(keyPair)
      message <- call(Abi.Request.EncodeMessage(targetAbi, None, Option(deploySet), Option(callSet), signer))
      _       <- sendGrams(message.address)
      target  <- call(Processing.Request.ProcessMessageWithoutEvents(MessageEncodeParams(targetAbi, signer, None, Option(deploySet), Option(callSet))))
      inputs     = Map("debotAbi" -> asHex(debotAbi.value).asJson, "targetAbi" -> asHex(targetAbi.value).asJson, "targetAddr" -> target.transaction.account_addr.get.asJson)
      dCallSet   = CallSet("constructor", None, Option(inputs))
      dDeploySet = DeploySet(debotTvc)
      dMessage <- call(Abi.Request.EncodeMessage(debotAbi, None, Option(dDeploySet), Option(dCallSet), signer))
      _        <- sendGrams(dMessage.address)
      debot    <- call(Processing.Request.ProcessMessageWithoutEvents(MessageEncodeParams(debotAbi, signer, None, Option(dDeploySet), Option(dCallSet))))
    } yield (debot.transaction.account_addr, target.transaction.account_addr, keyPair)
  }
}
