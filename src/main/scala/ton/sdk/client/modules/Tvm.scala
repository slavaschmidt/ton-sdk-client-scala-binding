package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding.{DecodedOutput, Fees, Transaction}
import ton.sdk.client.modules.Abi.AbiJson
import ton.sdk.client.binding.Api._

object Tvm {

  private val prefix = "tvm"

  case class AccountForExecutor(`type`: String, boc: Option[String], unlimited_balance: Option[Boolean])
  case class ExecutionOptions(blockchain_config: Option[String] = None, block_time: Option[Int] = None, block_lt: Option[Int] = None, transaction_lt: Option[Int] = None)

  val emptyExecutionOptions = ExecutionOptions()

  object AccountForExecutor {
    val none   = AccountForExecutor("None", None, None)
    val uninit = AccountForExecutor("Uninit", None, None)

    def from_account(boc: String, unlimited_balance: Option[Boolean]) = AccountForExecutor("Account", Option(boc), unlimited_balance)
  }

  object Request {
    final case class RunExecutor(
      message: String,
      account: AccountForExecutor,
      skip_transaction_check: Boolean = false,
      executionOptions: ExecutionOptions = emptyExecutionOptions,
      abi: Option[AbiJson] = None
    )
    final case class RunTvm(
      message: String,
      account: String,
      abi: Option[AbiJson] = None,
      executionOptions: ExecutionOptions = emptyExecutionOptions
    )
    final case class RunGet(account: String, function_name: String, input: Option[Json] = None, execution_options: ExecutionOptions = emptyExecutionOptions)
  }
  object Result {
    final case class RunExecutor(transaction: Transaction, out_messages: Seq[String], decoded: Option[DecodedOutput], account: String, fees: Fees)
  }

  import io.circe.generic.auto._
  import ton.sdk.client.binding.Decoders.decodeCompute

  implicit val runExecutor = new SdkCall[Request.RunExecutor, Result.RunExecutor] {
    override val function: String = s"$prefix.run_executor"
  }
  implicit val runTvm = new SdkCall[Request.RunTvm, Json] {
    override val function: String = s"$prefix.run_tvm"
  }
  implicit val runGet = new SdkCall[Request.RunGet, Json] {
    override val function: String = s"$prefix.run_get"
  }
}
