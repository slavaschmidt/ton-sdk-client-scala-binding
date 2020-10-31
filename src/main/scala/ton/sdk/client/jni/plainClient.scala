package ton.sdk.client.jni

import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

import scala.util.Try

final case class NetworkConfig(
  server_address:             Option[String] = None,
  network_retries_count:      Option[Int]    = None,
  message_retries_count:      Option[Int]    = None,
  message_processing_timeout: Option[Int]    = None,
  wait_for_timeout:           Option[Int]    = None,
  out_of_sync_threshold:      Option[BigInt] = None,
  access_key:                 Option[String] = None
)

final case class CryptoConfig(
  mnemonic_dictionary:   Option[Int]     = None,
  mnemonic_word_count:   Option[Int]     = None,
  hdkey_derivation_path: Option[String]  = None,
  hdkey_compliant:       Option[Boolean] = None
)

final case class AbiConfig(workchain: Option[Int], message_expiration_timeout: Option[Int], message_expiration_timeout_grow_factor: Option[Int])

final case class ClientConfig(network: Option[NetworkConfig] = None, crypto: Option[CryptoConfig] = None, abi: Option[AbiConfig] = None)

// The context should be closed after it is not needed any more
final case class Context private (id: Long) extends Closeable {
  private val open = new AtomicBoolean(true)

  @throws[Exception]
  override def close(): Try[Unit] = Try {
    Binding.tcDestroyContext(1)
    open.set(false)
  }

  override def finalize(): Unit = if (open.get()) close()
}

object Context {
  def apply(config: ClientConfig): Context = {
    Binding.tcCreateContext(config)
  }
}

