package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding.{ClientResult, OrderBy}
import ton.sdk.client.modules.Api.PlainSdkCall

// TODO Status: WIP
object Net {

  val prefix = "net"

  object Request {
    final case class Unsubscribe(handle: Int)
    final case class QueryCollection(collection: String, filter: Option[Json] = None, result: String = "", order: Option[Seq[OrderBy]] = None, limit: Option[Long] = None)
    final case class WaitForCollection(collection: String, filter: Option[Json] = None, result: String = "", timeout: Option[Long] = None)
    final case class SubscribeCollection(collection: String, filter: Option[Json] = None, result: String = "")
  }
  object Result {
    final case class QueryCollection(result: Seq[Json])
    final case class WaitForCollection(result: Json)
    final case class Handle(handle: Int, elements: LazyList[Json])
  }

  import io.circe.generic.auto._

  implicit val queryCollection = new PlainSdkCall[Request.QueryCollection, Result.QueryCollection] {
    override val functionName: String = s"$prefix.query_collection"
  }

  implicit val waitForCollection = new PlainSdkCall[Request.WaitForCollection, Result.WaitForCollection] {
    override val functionName: String = s"$prefix.wait_for_collection"
  }

  implicit val subscribeCollection = new PlainSdkCall[Request.SubscribeCollection, Result.Handle] {
    override val functionName: String = s"$prefix.subscribe_collection"
  }

  implicit val unsubscribe = new PlainSdkCall[Request.Unsubscribe, ClientResult[Int]] {
    override val functionName: String = s"$prefix.unsubscribe"
  }
}
