package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding.{ClientResult, Handle, OrderBy}
import ton.sdk.client.modules.Api.{SdkCall, StreamingSdkCall}

// TODO Status: WIP, needs refactoring of the callback
object Net {

  val prefix = "net"

  object Request {
    final case class Unsubscribe(handle: Long)
    final case class QueryCollection(collection: String, filter: Option[Json] = None, result: String = "", order: Option[Seq[OrderBy]] = None, limit: Option[Long] = None)
    final case class WaitForCollection(collection: String, filter: Option[Json] = None, result: String = "", timeout: Option[Long] = None)
    final case class SubscribeCollection(collection: String, filter: Option[Json] = None, result: String = "")
  }
  object Result {
    final case class QueryCollection(result: Seq[Json])
    final case class WaitForCollection(result: Json)
  }

  import io.circe.generic.auto._

  implicit val queryCollection = new SdkCall[Request.QueryCollection, Result.QueryCollection] {
    override val function: String = s"$prefix.query_collection"
  }

  implicit val waitForCollection = new SdkCall[Request.WaitForCollection, Result.WaitForCollection] {
    override val function: String = s"$prefix.wait_for_collection"
  }

  implicit val subscribeCollection = new StreamingSdkCall[Request.SubscribeCollection, Json] {
    override val function: String = s"$prefix.subscribe_collection"
  }

  implicit val unsubscribe = new SdkCall[Request.Unsubscribe, Json] {
    override val function: String = s"$prefix.unsubscribe"
  }
}
