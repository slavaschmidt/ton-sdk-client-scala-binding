package ton.sdk.client.binding

// Network endpoints: https://docs.ton.dev/86757ecb2/p/85c869-network-endpoints

final case class NetworkConfig(
  server_address: String,
  network_retries_count: Option[Int] = None,
  message_retries_count: Option[Int] = None,
  message_processing_timeout: Option[Int] = None,
  wait_for_timeout: Option[Int] = None,
  out_of_sync_threshold: Option[BigInt] = None,
  access_key: Option[String] = None
)

final case class CryptoConfig(
  mnemonic_dictionary: Option[Int] = None,
  mnemonic_word_count: Option[Int] = None,
  hdkey_derivation_path: Option[String] = None,
  hdkey_compliant: Option[Boolean] = None
)

final case class AbiConfig(workchain: Option[Int], message_expiration_timeout: Option[Int], message_expiration_timeout_grow_factor: Option[Int])

final case class ClientConfig(network: Option[NetworkConfig] = None, crypto: Option[CryptoConfig] = None, abi: Option[AbiConfig] = None)

object ClientConfig {
  def fromServer(server: String): ClientConfig = ClientConfig(Option(NetworkConfig(server)))
  val mainNet                                  = fromServer("main.ton.dev")
  val devNet                                   = fromServer("net.ton.dev")
  val testNet                                  = fromServer("testnet.ton.dev")
  val local                                    = fromServer("127.0.0.1")
}

// TODO make type-safe
//case class SortDirection(direction: String)
//object SortDirection {
//  val ASC  = SortDirection("ASC")
//  val DESC = SortDirection("DESC")
//}
final case class OrderBy(path: String, direction: String)

final case class ClientResult[T](result: T)

final case class KeyPair(public: String, secret: String)
