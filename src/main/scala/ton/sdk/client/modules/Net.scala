package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding.{Handle, OrderBy}
import ton.sdk.client.binding.Api.{SdkCall, StreamingSdkCall}

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

  object Request {
    final case class Unsubscribe(handle: Long)
    final case class Query(query: String, variables: Option[Json])
    final case class QueryCollection(collection: String, result: String, filter: Option[Json] = None, order: Option[Seq[OrderBy]] = None, limit: Option[Long] = None)
    final case class WaitForCollection(collection: String, result: String, filter: Option[Json] = None, timeout: Option[Long] = None)
    final case class SubscribeCollection(collection: String, result: String, filter: Option[Json] = None)
    final case class FindLastShardBlock(address: String)
    case object Suspend
    case object Resume
  }
  object Result {
    final case class Query(result: Json)
    final case class QueryCollection(result: Seq[Json])
    final case class WaitForCollection(result: Json)
    final case class LastShardBlock(block_id: String)
  }

  import io.circe.generic.auto._

  implicit val query                 = new SdkCall[Request.Query, Result.Query]                         { override val function: String = s"$module.query"                 }
  implicit val queryCollection       = new SdkCall[Request.QueryCollection, Result.QueryCollection]     { override val function: String = s"$module.query_collection"      }
  implicit val waitForCollection     = new SdkCall[Request.WaitForCollection, Result.WaitForCollection] { override val function: String = s"$module.wait_for_collection"   }
  implicit val subscribeCollection   = new StreamingSdkCall[Request.SubscribeCollection, Handle, Json]  { override val function: String = s"$module.subscribe_collection"  }
  implicit val unsubscribe           = new SdkCall[Request.Unsubscribe, Json]                           { override val function: String = s"$module.unsubscribe"           }
  implicit val suspend               = new SdkCall[Request.Suspend.type, Json]                          { override val function: String = s"$module.suspend"               }
  implicit val resume                = new SdkCall[Request.Resume.type, Json]                           { override val function: String = s"$module.resume"                }
  implicit val find_last_shard_block = new SdkCall[Request.FindLastShardBlock, Result.LastShardBlock]   { override val function: String = s"$module.find_last_shard_block" }
}
