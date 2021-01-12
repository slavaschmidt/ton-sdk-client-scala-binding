package ton.sdk.client.modules

import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax.EncoderOps
import org.scalatest.flatspec._
import ton.sdk.client.binding.Api.{DebotHandle, ResponseType, ResponseTypeAppNotify, ResponseTypeAppRequest}
import ton.sdk.client.binding.Context._
import ton.sdk.client.binding._
import ton.sdk.client.modules.Abi.AbiJson
import ton.sdk.client.modules.Debot.{DebotAction, DebotState, ParamsOfAppDebotBrowser, Result, ResultOfAppDebotBrowser}
import ton.sdk.client.modules.Processing.MessageEncodeParams

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}

sealed trait StartFn
case object Start extends StartFn
case object Fetch extends StartFn

case class Step(choice: Int, inputs: Seq[String], output: Seq[String], actions: Int, invokes: Seq[Step] = Nil)

class State(var handle: Option[DebotHandle], var messages: Seq[String], var actions: Seq[DebotAction], var steps: Seq[Step], var step: Option[Step], var done: Boolean = false, var switchStarted: Boolean = false) {
  def messageLog = messages.map(m => s"[LOG]\t$m").mkString("\n")
  def actionLog  = actions.map(m => s"[ACTION]\t$m").mkString("\n")

  def print(): Unit = println(messageLog + "\n" + actionLog)
}

class AsyncDebotSpec extends AsyncFlatSpec with SdkAssertions[Future] {
  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val ef: Context.Effect[Future]         = futureEffect
  implicit private val timeout                             = 1.minute

  private val predeployedData = (
    Some("0:7746883b560c4031d938df4d16b7901b0675e60ff9b5d9c62b10b76474073c41"),
    Some("0:d335e55f2a6cc430cf41301da6b01b652d9d3dbb0c3c92e3e4c4a5be3abd5882"),
    KeyPair("467514d850f711db030e5d585be8b21c108894aec3273ab3df7bd123c43eabc9", "f9316bc43d8fe45f9c29b0c56ee54ac3df19da96d00039120e1205fd7f30f23c")
  )

  private def redeployDebot = {
    val result = Await.result(devNet { implicit ctx =>
      deploy()
    }, 5.minutes)
    println(result)
    result
  }

  val (debotAddrOpt, targetAddrOpt, keys) = predeployedData
  val debotAddr                           = debotAddrOpt.get
  val targetAddr                          = targetAddrOpt.get

  behavior of "Debot"

  private def runSteps(steps: Seq[Step]) = {
    val result = devNet { implicit ctx =>
      Future(debotRun(steps, Start, Nil, debotAddr))
    }
    assertValue(result)(())
  }

  it should "run debot with print steps" in {
    runSteps(printSteps)
  }

  it should "run debot with goto steps" in {
    runSteps(gotoSteps)
  }

  it should "run debot with run steps" in {
    runSteps(runSteps)
  }

  private val gotoSteps = Seq(
    Step(0, Nil, Seq("Test Goto Action"), 1),
    Step(0, Nil, Seq("Debot Tests"), 8),
    Step(7, Nil, Nil, 0)
  )

  private val printSteps = Seq(
    Step(1, Nil, Seq("Test Print Action", "test2: instant print", "test instant print"), 3),
    Step(0, Nil, Seq("test simple print"), 3),
    Step(1, Nil, Seq(s"integer=1,addr=$targetAddr,string=test_string_1"), 3),
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
    Step(5, Seq(debotAddr), Seq("Test Invoke Debot Action", "enter debot address:"), 2),
    Step(0, Nil, Nil, 2, Seq(Step(0, Nil, Seq("Print test string", "Debot is invoked"), 0))),
    Step(1, Nil, Seq("Debot Tests"), 8),
    Step(7, Nil, Nil, 0)
  )

  def debotRun(steps: Seq[Step], startFn: StartFn, actions: Seq[DebotAction], address: String)(implicit ctx: Context): Unit = {
    val state    = new State(None, Nil, actions, steps, None)
    val callback = debotBrowser(address, keys)(state)

    val callResult: Future[Result.RegisteredDebot] = startFn match {
      case Start => callD(Debot.Request.Start(address), callback)
      case Fetch => callD(Debot.Request.Fetch(address), callback)
    }
    val debot = Await.result(callResult, timeout)
    state.handle = Option(debot.debot_handle)
    
    debotLoop(timeout, state, address)

    Await.ready(call(Debot.Request.Remove(state.handle.get)), timeout)
  }

  private def debotLoop(timeout: Duration, state: State, address: String)(implicit ctx: Context): Unit =
    state.steps match {
      case step :: tl if !state.done =>
        val action = state.actions(step.choice)
        println(s"[ACTION SELECTED]\t$action")
        state.steps = tl
        state.step = Option(step)
        state.messages = Nil
        val callback       = debotBrowser(address, keys)(state)
        val executeRequest = Debot.Request.Execute(state.handle.get, action)
        Await.ready(callD(executeRequest, callback), timeout)
        assert(state.messages.size == step.output.size)
        state.print()
      case _ =>
      // No steps or done
    }

  def debotBrowser(debotAddress: String, keyPair: KeyPair)(state: State)(implicit ctx: Context) =
    (responseType: ResponseType, responseData: Json) =>
      responseType match {
        case ResponseTypeAppNotify =>
          handleAppNotify(state, responseData)
        case ResponseTypeAppRequest =>
          handleAppRequest(keyPair, state, timeout, responseData.as[Map[String, Json]].toOption.get, debotAddress)
        case other =>
          fail(s"Unexpected response data $other")
      }

  private def handleAppRequest(keyPair: KeyPair, state: State, timeout: Duration, data: Map[String, Json], debotAddress: String)(implicit ctx: Context): Unit = {
    val browserParams = ParamsOfAppDebotBrowser(data("request_data")).get
    val result = browserParams match {
      case ParamsOfAppDebotBrowser.Input(_) =>
        ResultOfAppDebotBrowser.Input(value = state.step.get.inputs.head).asJson
      case ParamsOfAppDebotBrowser.GetSigningBox =>
        val box = Await.result(call(Crypto.Request.GetSigningBox(keyPair.public, keyPair.secret)), timeout)
        ResultOfAppDebotBrowser.GetSigningBox(box.handle).asJson
      case ParamsOfAppDebotBrowser.InvokeDebot(_, action) =>
        val steps = state.step.get.invokes
        debotRun(steps, Fetch, Seq(action), debotAddress)
        ResultOfAppDebotBrowser.InvokeDebot.asJson
    }
    val finalResult  = Client.AppRequest.Ok(result)
    val appRequestId = data("app_request_id").as[DebotHandle].toOption.get
    val _            = call(Client.Request.ResolveAppRequest(appRequestId, finalResult))
  }

  private def handleAppNotify(state: State, data: Json) = {
    ParamsOfAppDebotBrowser(data).get match {
      case ParamsOfAppDebotBrowser.Log(msg) =>
        state.messages = state.messages :+ msg
      case ParamsOfAppDebotBrowser.Switch(contextId) =>
        val done = contextId == DebotState.EXIT
        state.actions = Nil
        state.done = done
        state.switchStarted = true
      case ParamsOfAppDebotBrowser.SwitchCompleted(contextId) =>
        state.switchStarted = false
      case ParamsOfAppDebotBrowser.ShowAction(action) =>
        state.actions = state.actions :+ action
    }
  }

  private def deploy()(implicit ctx: Context): Future[(Option[String], Option[String], KeyPair)] = {
    val targetAbi = AbiJson.fromResource("DebotTarget.abi.json").toOption.get
    val debotAbi  = AbiJson.fromResource("Debot.abi.json").toOption.get
    val targetTvc = tvcFromResource("DebotTarget.tvc")
    val debotTvc  = tvcFromResource("Debot.tvc")
    val callSet   = CallSet("constructor")
    val deploySet = DeploySet(targetTvc)

    for {
      keyPair <- call(Crypto.Request.GenerateRandomSignKeys)
      _      = println(keyPair)
      signer = Signer.fromKeypair(keyPair)
      message <- call(Abi.Request.EncodeMessage(targetAbi, None, Option(deploySet), Option(callSet), signer))
      _       <- sendGrams(message.address)
      _ = println(s"Sent frst grams to ${message.address}")
      target <- call(Processing.Request.ProcessMessageWithoutEvents(MessageEncodeParams(targetAbi, signer, None, Option(deploySet), Option(callSet))))
      inputs     = Map("debotAbi" -> asHex(debotAbi.value).asJson, "targetAbi" -> asHex(targetAbi.value).asJson, "targetAddr" -> target.transaction.account_addr.get.asJson)
      dCallSet   = CallSet("constructor", None, Option(inputs))
      dDeploySet = DeploySet(debotTvc)
      dMessage <- call(Abi.Request.EncodeMessage(debotAbi, None, Option(dDeploySet), Option(dCallSet), signer))
      _        <- sendGrams(dMessage.address)
      _ = println(s"Sent scnd grams to ${dMessage.address}")
      debot <- call(Processing.Request.ProcessMessageWithoutEvents(MessageEncodeParams(debotAbi, signer, None, Option(dDeploySet), Option(dCallSet))))
    } yield (debot.transaction.account_addr, target.transaction.account_addr, keyPair)
  }
}
