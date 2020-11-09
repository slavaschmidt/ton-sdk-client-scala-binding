package ton.sdk.client.modules

import java.io.File
import java.nio.file.Files

import io.circe.Json
import io.circe.syntax.EncoderOps
import org.scalatest.flatspec.AsyncFlatSpec
import ton.sdk.client.binding.{Api, CallSet, Context, DeploySet, Signer}
import ton.sdk.client.modules.Abi._
import ton.sdk.client.binding.Context._
import ton.sdk.client.modules.Crypto._
import ton.sdk.client.modules.Processing.{Result, _}

import scala.concurrent.duration.DurationInt
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
        handle <- call(Processing.Request.processMessageS(params))
      } yield handle
    }
    while (!done) Thread.sleep(100)
    println(data)
    println(ef.unsafeGet(result))
    assert(data.out_messages.isEmpty && data.decoded.get.out_messages.isEmpty && data.decoded.get.output.isEmpty)
  }

  it should "wait_for_transaction with events" in {

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
        (handle, messages, errors) <- callS(Processing.Request.sendMessage(encoded.message, Option(abi))) // TODO should come the from callback
        _ = println(handle)
        msgs = messages.collect(20.seconds)
        // TODO implement properly
        // result <- callS(Processing.Request.waitForTransaction(encoded.message, shard_block_id.shard_block_id, Option(abi)))
        // _ = println(result)
      } yield msgs
    }
    // while (!done) Thread.sleep(100)
    println(ef.unsafeGet(result))
    fail()
    // TODO add assertion
    // assertExpression(result)(r => r.decoded.get.out_messages.isEmpty && r.decoded.get.output.isEmpty)
  }
}
