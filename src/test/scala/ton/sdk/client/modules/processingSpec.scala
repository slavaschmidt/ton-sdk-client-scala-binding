package ton.sdk.client.modules

import java.io.File
import java.nio.file.Files

import io.circe.syntax._
import org.scalatest.flatspec.AsyncFlatSpec
import ton.sdk.client.binding.{CallSet, Context, DeploySet, Signer}
import ton.sdk.client.modules.Abi._
import ton.sdk.client.binding.Context._
import ton.sdk.client.modules.Crypto._
import ton.sdk.client.modules.Processing._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

/**
  * Only async is supported by the client
  */
class processingSpec extends AsyncFlatSpec with SdkAssertions[Future] {

  implicit override def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit override val ef: Context.Effect[Future]         = futureEffect

  behavior of "Processing"

  private val abi       = AbiJson.fromResource("Events.abi.json", getClass.getClassLoader).toOption.get
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
        _    <- sendGrams(encoded.address)
        // Deploy account
        params = MessageEncodeParams(abi, signer, None, Option(deploySet), Option(callSet))
        account <- call(Processing.Request.ProcessMessageWithoutEvents(params))
      } yield account
    }
    assertExpression(result)(r => r.out_messages.isEmpty && r.decoded.get.out_messages.isEmpty && r.decoded.get.output.isEmpty)
  }

  // because of the slow test net
  ignore should "wait_for_transaction" in {
    val result = devNet { implicit ctx =>
      for {
        // Prepare data for deployment message
        keys <- call(Crypto.Request.GenerateRandomSignKeys)
        signer  = Signer.fromKeypair(keys)
        callSet = CallSet("constructor", Option(Map("pubkey" -> keys.public.asJson)), None)
        // Encode deployment message
        encoded <- call(Abi.Request.EncodeMessage(abi, None, Option(deploySet), Option(callSet), signer))
        sent    <- sendGrams(encoded.address)
        // Send message
        shard_block_id <- call(Processing.Request.SendMessageWithoutEvents(encoded.message, Option(abi)))
        result         <- call(Processing.Request.WaitForTransactionWithoutEvents(encoded.message, shard_block_id.shard_block_id, Option(abi)))
      } yield result
    }
    assertExpression(result)(r => r.decoded.get.out_messages.isEmpty && r.decoded.get.output.isEmpty)
  }

  ignore should "process_message with events" in {
    val result = devNet { implicit ctx =>
      for {
        // Prepare data for deployment message
        keys <- call(Crypto.Request.GenerateRandomSignKeys)
        signer  = Signer.fromKeypair(keys)
        callSet = CallSet("constructor", Option(Map("pubkey" -> keys.public.asJson)), None)
        // Encode deployment message
        encoded <- call(Abi.Request.EncodeMessage(abi, None, Option(deploySet), Option(callSet), signer))
        _       <- sendGrams(encoded.address)
        // Deploy account
        params = MessageEncodeParams(abi, signer, None, Option(deploySet), Option(callSet))
        (data, messages, _) <- callS(Processing.Request.ProcessMessageWithEvents(params))
        // Check that messages are indeed received
        _ = assert(messages.collect(1.minute).nonEmpty)
      } yield data
    }
    assertExpression(result)(data => data.out_messages.isEmpty && data.decoded.get.out_messages.isEmpty && data.decoded.get.output.isEmpty)
  }

  ignore should "wait_for_transaction with events" in {
    val result = devNet { implicit ctx =>
      for {
        // Prepare data for deployment message
        keys <- call(Crypto.Request.GenerateRandomSignKeys)
        signer  = Signer.fromKeypair(keys)
        callSet = CallSet("constructor", Option(Map("pubkey" -> keys.public.asJson)), None)
        // Encode deployment message
        encoded <- call(Abi.Request.EncodeMessage(abi, None, Option(deploySet), Option(callSet), signer))
        _       <- sendGrams(encoded.address)
        // Send message
        (shardBlock, _, _) <- callS(Processing.Request.SendMessageWithEvents(encoded.message, Option(abi)))
        (result, _, _) <- callS(Processing.Request.WaitForTransactionWithEvents(encoded.message, shardBlock.shard_block_id, Option(abi)))
      } yield result
    }
    assertExpression(result)(r => r.out_messages.isEmpty && r.decoded.get.out_messages.isEmpty && r.decoded.get.output.isEmpty)
  }

}
