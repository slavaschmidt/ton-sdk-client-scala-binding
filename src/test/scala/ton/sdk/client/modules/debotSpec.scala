package ton.sdk.client.modules

import io.circe.Json
import io.circe.generic.auto._
import io.circe.literal.JsonStringContext
import io.circe.syntax.EncoderOps
import org.scalatest.Ignore
import org.scalatest.flatspec._
import ton.sdk.client.binding.Api.{ResponseType, ResponseTypeAppNotify, ResponseTypeAppRequest, ResponseTypeResult}
import ton.sdk.client.binding.Context._
import ton.sdk.client.binding._
import ton.sdk.client.modules.Abi.AbiJson
import ton.sdk.client.modules.Debot.{DebotAction, DebotHandle, DebotState, ParamsOfAppDebotBrowser, Result, ResultOfAppDebotBrowser}
import ton.sdk.client.modules.Processing.MessageEncodeParams
import ton.sdk.client.modules.Utils._

import scala.concurrent.duration.{Duration, DurationInt}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.higherKinds

case class Step(choice: Int, inputs: Seq[String], output: Seq[String], actions: Int, invokes: Seq[Step] = Nil)
case class State(handle: Option[DebotHandle], messages: Seq[String], actions: Seq[DebotAction], steps: Seq[Step], step: Option[Step], done: Boolean = false)

@Ignore
class AsyncDebotSpec extends AsyncFlatSpec with SdkAssertions[Future] {
  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val ef: Context.Effect[Future]         = futureEffect

  behavior of "Debot"

  private implicit val timeout = 1.minute

  it should "run debot with steps" in {
    val result = local { implicit ctx =>
      Future(debotRun(gotoSteps, "start", Nil))
    }
    assertValue(result)(null)
  }

  private val gotoSteps = Seq(
    Step(0, Nil, Seq("Test Goto Action"), 1),
    Step(0, Nil, Seq("Debot Tests"), 8),
    Step(7, Nil, Nil, 0)
  )

  private def printSteps(targetAddress: String) = Seq(
    Step(1, Nil, Seq("Test Print Action", "test2: instant print", "test instant print"), 3),
    Step(0, Nil, Seq("test simple print"), 3),
    Step(1, Nil, Seq(s"integer=1,addr=$targetAddress,string=test_string_1"), 3),
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

  private def invokeSteps(debotAddress: String) = Seq(
    Step(5, Seq(debotAddress), Seq("Test Invoke Debot Action", "enter debot address:"), 2),
    Step(0, Nil, Nil, 2, Seq(Step(0, Nil, Seq("Print test string", "Debot is invoked"), 0))),
    Step(1, Nil, Seq("Debot Tests"), 8),
    Step(7, Nil, Nil, 0)
  )

  def debotRun(steps: Seq[Step], startFn: String, actions: Seq[DebotAction])(implicit ctx: Context, timeout: Duration): State= {
    val state = State(None, Nil, actions, steps, None)
    // TODO asyncio.get_running_loop().create_task(debotBrowser()(state, startFn))
    // TODO await self._debot_handle_await(state) // until there is state['handle']
    debotPrintState(state)

    while(state.steps.nonEmpty) state.steps match {
      case step :: tl =>
        val action = state.actions(step.choice)
        println(s"[ACTION SELECTED]\t$action")
        val newState = state.copy(steps = tl, step = Option(step), messages = Nil)
        Await.ready(call(Debot.Request.Execute(state.handle.get, action)), timeout)
        // TODO if (state.messages.size != step.output.size || state.actions.size != step.actions) asyncio.sleep(1)
        debotPrintState(state)
    }
    call(Debot.Request.Remove(state.handle.get))
    ??? // TODO fixme
  }


  // TODO feels like state should be shared and modified
  def debotBrowser(debotAddress: Debot.Address, keyPair: KeyPair)(state: State, startFn: String)(implicit ctx: Context, timeout: Duration): State = {
    val fEvents: Future[(Result.RegisteredDebot, BlockingIterator[Json], BlockingIterator[Api.SdkClientError])] = startFn match {
      case "start" => callS(Debot.Request.Start(debotAddress))
      case "fetch" => callS(Debot.Request.Fetch(debotAddress))
    }

    val (debot, events, errors) = Await.result(fEvents, timeout)

    val cursor = events.getNext(1.minute).hcursor

    cursor.get[Map[String, Json]]("response_data") match {
      case Left(value) => state
      case Right(data) =>
        cursor.get[Long]("response_type").map(ResponseType.apply) match {
          case Right(ResponseTypeResult) =>
            state.copy(handle = Option(debot.debot_handle)) // data("debot_handle")

          case Right(ResponseTypeAppNotify) =>
            ParamsOfAppDebotBrowser(data) match {
              case ParamsOfAppDebotBrowser.Log(msg) =>
                state.copy(messages = state.messages :+ msg)
              case ParamsOfAppDebotBrowser.Switch(contextId) =>
                val done = contextId == DebotState.EXIT
                state.copy(actions = Nil, done = done)
              case ParamsOfAppDebotBrowser.ShowAction(action) =>
                val debotAction = ??? // DebotAction() // TODO FIXME
                state.copy(actions = state.actions :+ debotAction)
            }

          case Right(ResponseTypeAppRequest) =>
            val (result, newState) = ParamsOfAppDebotBrowser(data("request_data").as[Map[String, Json]].toOption.get) match {
              case ParamsOfAppDebotBrowser.Input(_) =>
                (ResultOfAppDebotBrowser.Input(value = state.step.get.inputs.head).asJson, state)
              case ParamsOfAppDebotBrowser.GetSigningBox =>
                val box = Await.result(call(Crypto.Request.GetSigningBox(keyPair.public, keyPair.secret)), timeout)
                (ResultOfAppDebotBrowser.GetSigningBox(box.handle).asJson, state)
              case ParamsOfAppDebotBrowser.InvokeDebot(_, action) =>
                val debotAction = ??? // DebotAction() // TODO FIXME
                val newState = debotRun(steps = state.step.get.invokes, "fetch", Seq(debotAction))
                (ResultOfAppDebotBrowser.InvokeDebot.asJson, newState)
              case _ => (json"""{}""", state)
            }
            val finalResult  = Client.AppRequest.Ok(result)
            val appRequestId = data("app_request_id").as[Int].toOption.get
            val _ = call(Client.Request.ResolveAppRequest(appRequestId, finalResult))
            newState
        }
    }
  }

  def debotPrintState(state: State) = {
    println(state.messages.map(m => s"[LOG]\t$m").mkString("\n")) // TODO write log logging.info(')
    println(state.actions.map(m => s"[ACTION]\t$m").mkString("\n")) // TODO write log logging.info(')
  }

  def deploy()(implicit ctx: Context) = {

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
    } yield (debot.transaction.account_addr, target.transaction.account_addr)
  }
}
