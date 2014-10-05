package processes.freeMonads.multiple

import play.api.libs.json.JsPath
import play.api.data.validation.ValidationError
import play.api.mvc.Result
import play.api.mvc.Request
import play.api.libs.json.JsValue
import domain.Profile
import play.api.mvc.AnyContent
import play.api.libs.json.JsResult

trait CompleteProgramParts {
  
  sealed trait Json[T]
  case class ParseJson(request: Request[AnyContent]) extends Json[Option[JsValue]]
  case class JsonToProfile(json: JsValue) extends Json[JsResult[Profile]]
  
  sealed trait Store[T]
  case class GetProfileById(id: String) extends Store[Option[Profile]]
  case class UpdateProfile(id: String, profile: Profile) extends Store[Unit]
  
  sealed trait Utilities[T]
  case class MergeProfile(oldProfile: Profile, newProfile: Profile) extends Utilities[Profile]
  
  sealed trait Http[T]
  case object BadRequest extends Http[Result]
  case class ValidationErrors(errors: Seq[(JsPath, Seq[ValidationError])]) extends Http[Result]
  case class NotFound(id: String) extends Http[Result]
  
  class Branch[L] {
    case class Instance[R](value: L)
  }
  object Branch {
    def apply[L, R](value: L) = new Branch[L].Instance[R](value)
  }
}