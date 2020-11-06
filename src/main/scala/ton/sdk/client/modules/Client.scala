package ton.sdk.client.modules

import io.circe.{Decoder, Json}
import ton.sdk.client.modules.Api.{SdkCall, SdkResultOrError}

object Client {

  object Request {
    final case object Version
    final case object BuildInfo
    final case object ApiReference
  }
  object Result {
    final case class Version(version: String)
    final case class BuildInfo(build_info: Json)

    case class GenericArg(`type`: String, ref_name: String)
    case class Param(name: String, `type`: String, generic_name: String, generic_args: List[GenericArg], summary: Option[String], description: Option[String])
    case class FnResult(`type`: String, generic_name: String, generic_args: List[GenericArg])
    case class StructField(name: String, `type`: String, summary: Option[String], description: Option[String], ref_name: Option[String])
    case class Functions(name: String, summary: Option[String], description: Option[String], params: List[Param], result: FnResult, errors: Option[String])
    case class Types(
      name: String,
      `type`: String,
      struct_fields: Option[Seq[StructField]],
      enum_types: Option[Seq[StructField]],
      summary: Option[String],
      description: Option[String]
    )
    case class Module(name: String, summary: Option[String], description: Option[String], types: List[Types], functions: List[Json])
    case class Api(version: String, modules: List[Module])
    case class ApiReference(api: Api)
  }

  import io.circe.generic.auto._

  implicit val version = new SdkCall[Request.Version.type, Result.Version] {
    override val functionName: String = "client.version"
  }

  implicit val buildInfo = new SdkCall[Request.BuildInfo.type, Result.BuildInfo] {
    override val functionName: String = "client.build_info"
  }

  implicit val apiReference = new SdkCall[Request.ApiReference.type, Result.ApiReference] {
    override val functionName: String = "client.get_api_reference"
  }
}
