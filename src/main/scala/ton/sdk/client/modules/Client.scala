package ton.sdk.client.modules

import io.circe.Json
import ton.sdk.client.binding.Api.SdkCall

/**
  * Module client
  *
  * Provides information about library.
  *
  * Please refer to the [[https://github.com/tonlabs/TON-SDK/blob/master/docs/mod_client.md SDK documentation]]
  * for the detailed description of individual functions and parameters
  *
  */
// scalafmt: { maxColumn = 300 }
object Client {

  private val module = "client"

  final case class GenericArg(`type`: String, ref_name: String)
  final case class Param(name: String, `type`: String, generic_name: String, generic_args: List[GenericArg], summary: Option[String], description: Option[String])
  final case class FnResult(`type`: String, generic_name: String, generic_args: List[GenericArg])
  final case class StructField(name: String, `type`: String, summary: Option[String], description: Option[String], ref_name: Option[String])
  final case class Functions(name: String, summary: Option[String], description: Option[String], params: List[Param], result: FnResult, errors: Option[String])
  final case class Types(name: String, `type`: String, struct_fields: Option[Seq[StructField]], enum_types: Option[Seq[StructField]], summary: Option[String], description: Option[String])
  final case class Module(name: String, summary: Option[String], description: Option[String], types: List[Types], functions: List[Json])
  final case class ApiDescription(version: String, modules: List[Module])
  final case class Dependency(name: String, git_commit: String)
  final case class AppRequestResult(`type`: String, text: Option[String], result: Option[Json])
  object AppRequest {
    def Error(text: String) = AppRequestResult("Error", Option(text), None)
    def Ok(result: Json)    = AppRequestResult("Ok", None, Option(result))
  }

  object Request {
    final case object Version
    final case object BuildInfo
    final case object ApiReference
    final case class ResolveAppRequest(app_request_id: Int, result: AppRequestResult)
  }
  object Result {
    final case class Version(version: String)
    final case class BuildInfo(build_number: Long, dependencies: Seq[Dependency])
    final case class ApiReference(api: ApiDescription)
  }

  import io.circe.generic.auto._

  implicit val apiReference      = new SdkCall[Request.ApiReference.type, Result.ApiReference] { override val function: String = s"$module.get_api_reference"   }
  implicit val version           = new SdkCall[Request.Version.type, Result.Version]           { override val function: String = s"$module.version"             }
  implicit val buildInfo         = new SdkCall[Request.BuildInfo.type, Result.BuildInfo]       { override val function: String = s"$module.build_info"          }
  implicit val resolveAppRequest = new SdkCall[Request.ResolveAppRequest, Unit]                { override val function: String = s"$module.resolve_app_request" }

}
