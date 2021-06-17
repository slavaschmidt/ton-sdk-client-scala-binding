package ton.sdk.client.modules

import io.circe.{Encoder, Json}
import ton.sdk.client.binding.{Handle, OrderBy}
import ton.sdk.client.binding.Api.{SdkCall, StreamingSdkCall}
import ton.sdk.client.modules.Abi.Result.DecodedMessageBody
import ton.sdk.client.modules.Net.Request.QueryTransactionTree

/**
  * Module net
  *
  * Network access.
  *
  * Please refer to the [[https://github.com/tonlabs/TON-SDK/blob/master/docs/mod_net.md SDK documentation]]
  * for the detailed description of individual functions and parameters
  *
  */
// scalafmt: { maxColumn = 300 }
object Net {

  private val module = "net"

  final case class EndpointsSet(endpoints: Seq[String])
  final case class FieldAggregation(field: String, fn: String)

  sealed trait QueryParams {
    def collection: String
    def filter: Option[Json]
  }

  final case class AbiParam(name: String, `type`: String, components: Option[Seq[AbiParam]])
  final case class AbiData(key: Long, name: String, `type`: String, components: Option[Seq[AbiParam]])
  final case class AbiEvent(name: String, inputs: Seq[AbiParam], id: Option[String])
  final case class AbiFunction(name: String, inputs: Seq[AbiParam], outputs: Seq[AbiParam], id: Option[String])
  final case class AbiContract(`ABI version`: Option[Int], abi_version: Option[Int], header: Option[Seq[String]], functions: Option[Seq[AbiFunction]], events: Option[Seq[AbiEvent]], data: Option[Seq[AbiData]])

  sealed trait Abi[T] {
    val `type`: String
    val value: T
  }
  final case class Abi_Json(value: String) extends Abi[String] {
    override val `type` = "Json"
  }
  final case class Abi_Contract(value: AbiContract) extends Abi[AbiContract] {
    override val `type` = "Contract"
  }
  final case class Abi_Handle(value: BigInt) extends Abi[BigInt] {
    override val `type` = "Handle"
  }
  final case class Abi_Serialized(value: AbiContract) extends Abi[AbiContract] {
    override val `type` = "Serialized"
  }

  final case class MessageNode(id: String, src_transaction_id: Option[String], dst_transaction_id: Option[String], src: Option[String], dst: Option[String], value: Option[String], bounce: Boolean, decoded_body: Option[DecodedMessageBody])
  final case class TransactionNode(id: String, in_msg: String, out_msgs: Seq[String], account_addr: String, total_fees: String, aborted: Boolean, exit_code: Int)

  object Request {
    final case class Unsubscribe(handle: Long)
    final case class Query(query: String, variables: Option[Json])
    final case class QueryCollection(collection: String, result: String, filter: Option[Json] = None, order: Option[Seq[OrderBy]] = None, limit: Option[Long] = None) extends QueryParams
    final case class WaitForCollection(collection: String, result: String, filter: Option[Json] = None, timeout: Option[Long] = None)                                 extends QueryParams
    final case class SubscribeCollection(collection: String, result: String, filter: Option[Json] = None)
    final case class FindLastShardBlock(address: String)
    case object FetchEndpoints
    final case class SetEndpoints(endpoints: Seq[String])
    final case class AggregateCollection(collection: String, filter: Option[Json], fields: Option[Seq[FieldAggregation]]) extends QueryParams
    final case class BatchQuery(operations: Seq[QueryParams])
    final case class QueryCounterparties(account: String, result: String, first: Option[Int], after: Option[String])
    final case class QueryTransactionTree(in_msg: String, abi_registry: Option[Seq[Abi[_]]], timeout: Long)
    case object Suspend
    case object Resume
  }
  object Result {
    final case class Query(result: Json)
    final case class QueryCollection(result: Seq[Json])
    final case class WaitForCollection(result: Json)
    final case class LastShardBlock(block_id: String)
    final case class CollectionAggregation(values: Seq[Json])
    final case class BatchQuery(results: Seq[Json])
    final case class TransactionTree(messages: Seq[MessageNode], transactions: Seq[TransactionNode])
  }

  import io.circe.generic.extras.auto._
  import io.circe.generic.extras.Configuration
  implicit val encodeAbi: Encoder[Net.Abi[_]] = Encoder.instance {
    case abi: Abi_Json       => Encoder[Abi_Json].apply(abi)
    case abi: Abi_Handle     => Encoder[Abi_Handle].apply(abi)
    case abi: Abi_Contract   => Encoder[Abi_Contract].apply(abi)
    case abi: Abi_Serialized => Encoder[Abi_Serialized].apply(abi)
  }
  implicit val encodeQueryTransactionTree: Encoder[QueryTransactionTree] =
    Encoder.forProduct2("in_msg", "abi_registry")(u => (u.in_msg, u.abi_registry))

  implicit val genDevConfig: Configuration = Configuration.default.withDiscriminator("type")

  implicit val query                  = new SdkCall[Request.Query, Result.Query]                               { override val function: String = s"$module.query"                  }
  implicit val queryCollection        = new SdkCall[Request.QueryCollection, Result.QueryCollection]           { override val function: String = s"$module.query_collection"       }
  implicit val waitForCollection      = new SdkCall[Request.WaitForCollection, Result.WaitForCollection]       { override val function: String = s"$module.wait_for_collection"    }
  implicit val subscribeCollection    = new StreamingSdkCall[Request.SubscribeCollection, Handle, Json]        { override val function: String = s"$module.subscribe_collection"   }
  implicit val unsubscribe            = new SdkCall[Request.Unsubscribe, Json]                                 { override val function: String = s"$module.unsubscribe"            }
  implicit val aggregateCollection    = new SdkCall[Request.AggregateCollection, Result.CollectionAggregation] { override val function: String = s"$module.aggregate_collection"   }
  implicit val batchQuery             = new SdkCall[Request.BatchQuery, Result.BatchQuery]                     { override val function: String = s"$module.batch_query"            }
  implicit val suspend                = new SdkCall[Request.Suspend.type, Json]                                { override val function: String = s"$module.suspend"                }
  implicit val resume                 = new SdkCall[Request.Resume.type, Json]                                 { override val function: String = s"$module.resume"                 }
  implicit val find_last_shard_block  = new SdkCall[Request.FindLastShardBlock, Result.LastShardBlock]         { override val function: String = s"$module.find_last_shard_block"  }
  implicit val fetch_endpoints        = new SdkCall[Request.FetchEndpoints.type, EndpointsSet]                 { override val function: String = s"$module.fetch_endpoints"        }
  implicit val set_endpoints          = new SdkCall[Request.SetEndpoints, Unit]                                { override val function: String = s"$module.set_endpoints"          }
  implicit val query_counterparties   = new SdkCall[Request.QueryCounterparties, Result.QueryCollection]       { override val function: String = s"$module.query_counterparties"   }
  implicit val query_transaction_tree = new SdkCall[Request.QueryTransactionTree, Result.TransactionTree]      { override val function: String = s"$module.query_transaction_tree" }

}
