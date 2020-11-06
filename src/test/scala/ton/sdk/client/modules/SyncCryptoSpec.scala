package ton.sdk.client.modules

import ton.sdk.client.modules.Context.tryEffect
import scala.util.Try

class SyncCryptoSpec extends CryptoSpec[Try] {
  implicit override val fe: Context.Effect[Try] = tryEffect
}
