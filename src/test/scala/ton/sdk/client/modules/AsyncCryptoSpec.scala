package ton.sdk.client.modules


import ton.sdk.client.modules.Context.futureEffect

import scala.concurrent.{ExecutionContext, Future}

class AsyncCryptoSpec extends CryptoSpec[Future] {
  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  override implicit val fe: Context.Effect[Future] = futureEffect

}
