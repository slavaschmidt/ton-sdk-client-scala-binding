package ton.sdk.client.modules

import io.circe.{Decoder, Json}
import ton.sdk.client.modules.Api.{SdkCall, SdkResultOrError}

object Client {
  // Network endpoints: https://docs.ton.dev/86757ecb2/p/85c869-network-endpoints

  final case class NetworkConfig(
    servers: Seq[String],
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
    def fromServer(server: String): ClientConfig = ClientConfig(Option(NetworkConfig(Seq(server))))
    val mainNet                                  = fromServer("main.ton.dev")
    val devNet                                   = fromServer("net.ton.dev")
    val testNet                                  = fromServer("testnet.ton.dev")
    val local                                    = fromServer("127.0.0.1")
  }

  object Request {
    final case object Version
    final case object BuildInfo
    final case object ApiReference
  }
  object Result {
    final case class Version(version: String)
    final case class BuildInfo(build_info: Json)

    case class Generic_args(`type`: String, ref_name: String)
    case class Params(name: String, `type`: String, generic_name: String, generic_args: List[Generic_args], summary: String, description: String)
    //case class FnResult(`type`: String, generic_name: String, generic_args: List[Generic_args])
    case class StructField(name: String, `type`: String, summary: Option[String], description: Option[String], ref_name: Option[String])
    case class Functions(name: String, summary: Option[String], description: Option[String], params: List[Json], result: Json, errors: Option[String])
    case class Types(name: String, `type`: String, struct_fields: Option[Seq[StructField]], enum_types: Option[Seq[StructField]], summary: Option[String], description: Option[String])
    case class Module(name: String, summary: Option[String], description: Option[String], types: List[Types], functions: List[Json])
    case class Api(version: String, modules: List[Module])
    case class ApiReference(api: Api)
  }

  import io.circe.generic.auto._

  implicit val version = new SdkCall[Request.Version.type, Result.Version] {
    override val functionName: String = "client.version"
    type R = Result.Version
  }

  implicit val buildInfo = new SdkCall[Request.BuildInfo.type, Result.BuildInfo] {
    override val functionName: String = "client.build_info"
    type R = Result.Version
  }

  implicit val apiReference = new SdkCall[Request.ApiReference.type, Result.ApiReference] {
    override val functionName: String = "client.get_api_reference"
    type R = Result.Version
  }
}
