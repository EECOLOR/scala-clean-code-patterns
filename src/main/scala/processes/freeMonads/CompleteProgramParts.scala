package processes.freeMonads

import play.api.libs.json.JsPath
import play.api.data.validation.ValidationError
import play.api.mvc.Result
import play.api.mvc.Request
import play.api.libs.json.JsValue
import domain.Profile
import play.api.mvc.AnyContent
import play.api.libs.json.JsResult

trait CompleteProgramParts {

  sealed trait Method[ReturnType]
  case class ParseJson(request: Request[AnyContent]) extends Method[Option[JsValue]]
  case class JsonToProfile(json: JsValue) extends Method[JsResult[Profile]]
  case class GetProfileById(id: String) extends Method[Option[Profile]]
  case class MergeProfile(oldProfile: Profile, newProfile: Profile) extends Method[Profile]
  case class UpdateProfile(id: String, profile: Profile) extends Method[Unit]
  case class WrappedOption[A](wrapped: Method[Option[A]], ifEmpty: Result) extends Method[A]
  case class WrappedJsResult[A](
    wrapped: Method[JsResult[A]],
    jsErrorsToResult: Seq[(JsPath, Seq[ValidationError])] => Result) extends Method[A]

  implicit class OptionMethodEnhancements[A](m: Method[Option[A]]) {
    def ifEmpty(result: Result): Method[A] = WrappedOption(m, result)
  }

  implicit class JsResultMethodEnhancements[A](m: Method[JsResult[A]]) {
    def ifError(jsErrorsToResult: Seq[(JsPath, Seq[ValidationError])] => Result): Method[A] =
      WrappedJsResult(m, jsErrorsToResult)
  }
}