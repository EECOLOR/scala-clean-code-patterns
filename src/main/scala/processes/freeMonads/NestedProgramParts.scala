package processes.freeMonads

import domain.Profile
import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import processes.PatchAssignment

trait NestedProgramParts { _: PatchAssignment =>

  sealed trait ServiceResult
  case object Success extends ServiceResult
  case object BadRequest extends ServiceResult
  case class ValidationErrors(errors: Seq[(JsPath, Seq[ValidationError])]) extends ServiceResult
  case class NotFound(id: String) extends ServiceResult

  sealed trait Service[ReturnType]
  case class Return(result: Result) extends Service[Result]
  case class ParseJson(request: Request[AnyContent]) extends Service[Option[JsValue]]
  case class JsonToProfile(json: JsValue) extends Service[JsResult[Profile]]
  case class GetProfileById(id: String) extends Service[Option[Profile]]
  case class MergeProfile(oldProfile: Profile, newProfile: Profile) extends Service[Profile]
  case class UpdateProfile(id: String, profile: Profile) extends Service[Unit]
  case class WrappedOption[A](wrapped: Service[Option[A]], ifEmpty: ServiceResult) extends Service[A]
  case class WrappedJsResult[A](
    wrapped: Service[JsResult[A]],
    validationErrors: Seq[(JsPath, Seq[ValidationError])] => ServiceResult) extends Service[A]

  implicit class JsResultServiceEnhancements[A](m: Service[JsResult[A]]) {
    def ifError(jsErrorsToResult: Seq[(JsPath, Seq[ValidationError])] => ServiceResult): Service[A] =
      WrappedJsResult(m, jsErrorsToResult)
  }

  implicit class OptionServiceEnhancements[A](m: Service[Option[A]]) {
    def ifEmpty(result: ServiceResult): Service[A] =
      WrappedOption(m, result)
  }

}