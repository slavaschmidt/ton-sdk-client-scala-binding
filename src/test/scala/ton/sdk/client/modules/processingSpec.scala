package ton.sdk.client.modules

import java.io.File
import java.nio.file.Files

import io.circe.syntax.EncoderOps
import org.scalatest.flatspec.AsyncFlatSpec
import ton.sdk.client.binding.{CallSet, DeploySet, Signer}
import ton.sdk.client.modules.Abi._
import ton.sdk.client.modules.Context._
import ton.sdk.client.modules.Crypto._
import ton.sdk.client.modules.Processing._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Only async is supported by the client
  */
// TODO: status - needs refactoring of the callback
class AsyncProcessingSpec extends AsyncFlatSpec with SdkAssertions[Future] {

  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val ef: Context.Effect[Future]         = futureEffect

  behavior of "Processing"

  private val abi       = AbiJson.fromResource("Events.abi.json").toOption.get
  private val tvcSrc    = Files.readAllBytes(new File(getClass.getClassLoader.getResource("Events.tvc").getFile).toPath)
  private val tvc       = base64(tvcSrc)
  private val deploySet = DeploySet(tvc)

  it should "process_message" in {
    val result = devNet { implicit ctx =>
      for {
        // Prepare data for deployment message
        keys <- call(Crypto.Request.GenerateRandomSignKeys)
        signer  = Signer.fromKeypair(keys)
        callSet = CallSet("constructor", Option(Map("pubkey" -> keys.public.asJson)), None)
        // Encode deployment message
        encoded <- call(Abi.Request.EncodeMessage(abi, None, Option(deploySet), Option(callSet), signer))
        sent <- sendGrams(encoded.address)
        // Deploy account
        params = MessageEncodeParams(abi, signer, None, Option(deploySet), Option(callSet))
        account <- call(Processing.Request.processMessage(params))
        _ = println(account)
      } yield account
    }
    assertExpression(result)(r => r.out_messages.isEmpty && r.decoded.get.out_messages.isEmpty && r.decoded.get.output.isEmpty)
  }

  it should "wait_for_transaction" in {
    val result = devNet { implicit ctx =>
      for {
        // Prepare data for deployment message
        keys <- call(Crypto.Request.GenerateRandomSignKeys)
        signer  = Signer.fromKeypair(keys)
        callSet = CallSet("constructor", Option(Map("pubkey" -> keys.public.asJson)), None)
        // Encode deployment message
        encoded <- call(Abi.Request.EncodeMessage(abi, None, Option(deploySet), Option(callSet), signer))
        sent <- sendGrams(encoded.address)
        // Send message
        shard_block_id <- call(Processing.Request.sendMessage(encoded.message, Option(abi)))
        result <- call(Processing.Request.waitForTransaction(encoded.message, shard_block_id.shard_block_id, Option(abi)))
        _ = println(result)
      } yield result
    }
    assertExpression(result)(r => r.decoded.get.out_messages.isEmpty && r.decoded.get.output.isEmpty)
  }

  it should "process_message with events" in {
    var data: Processing.Result.ResultOfProcessMessage = null
    var done = false
    val callback = (finished: Boolean, tpe: Long, in: Processing.Result.ResultOfProcessMessage) => {
      println(s"$done: $in")
      data = in
      done = finished
    }

    val result = devNet { implicit ctx =>
      for {
        // Prepare data for deployment message
        keys <- call(Crypto.Request.GenerateRandomSignKeys)
        signer  = Signer.fromKeypair(keys)
        callSet = CallSet("constructor", Option(Map("pubkey" -> keys.public.asJson)), None)
        // Encode deployment message
        encoded <- call(Abi.Request.EncodeMessage(abi, None, Option(deploySet), Option(callSet), signer))
        sent <- sendGrams(encoded.address)
        // Deploy account
        params = MessageEncodeParams(abi, signer, None, Option(deploySet), Option(callSet))
        handle <- call(Processing.Request.processMessage(params, callback))
      } yield handle
    }
    while (!done) Thread.sleep(100)
    println(data)
    println(ef.unsafeGet(result))
    assert(data.out_messages.isEmpty && data.decoded.get.out_messages.isEmpty && data.decoded.get.output.isEmpty)
  }

  it should "wait_for_transaction with events" in {
    var data: Processing.Result.ResultOfProcessMessage = null
    var done = false
    val callback = (finished: Boolean, tpe: Long, in: Processing.Result.ResultOfProcessMessage) => {
      println(s"waitfortransaction $done: $in")
      data = in
      done = finished
    }
    var msgData: Processing.Result.SendMessage = null
    val sendMsgCallback = (finished: Boolean, tpe: Long, in: Processing.Result.SendMessage) => {
      println(s"sendmsg $done: $in")
      msgData = in
      done = finished
    }

    val result = devNet { implicit ctx =>
      for {
        // Prepare data for deployment message
        keys <- call(Crypto.Request.GenerateRandomSignKeys)
        signer  = Signer.fromKeypair(keys)
        callSet = CallSet("constructor", Option(Map("pubkey" -> keys.public.asJson)), None)
        // Encode deployment message
        encoded <- call(Abi.Request.EncodeMessage(abi, None, Option(deploySet), Option(callSet), signer))
        sent <- sendGrams(encoded.address)
        // Send message
        shard_block_id <- call(Processing.Request.sendMessage(encoded.message, Option(abi), sendMsgCallback)) // TODO should come the from callback
        result <- call(Processing.Request.waitForTransaction(encoded.message, shard_block_id.shard_block_id, Option(abi), callback))
        _ = println(result)
      } yield result
    }
    // while (!done) Thread.sleep(100)
    println(data)
    println(ef.unsafeGet(result))
    assertExpression(result)(r => r.decoded.get.out_messages.isEmpty && r.decoded.get.output.isEmpty)
  }
}
