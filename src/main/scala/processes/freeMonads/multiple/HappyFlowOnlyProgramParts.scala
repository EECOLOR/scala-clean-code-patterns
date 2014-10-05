package processes.freeMonads.multiple

import domain.Profile
import play.api.libs.json.JsValue
import play.api.mvc.Request
import play.api.mvc.AnyContent

trait HappyFlowOnlyProgramParts {

  sealed trait Json[T]
  case class ParseJson(request: Request[AnyContent]) extends Json[JsValue]
  case class JsonToProfile(json: JsValue) extends Json[Profile]
  
  sealed trait Store[T]
  case class GetProfileById(id: String) extends Store[Profile]
  case class UpdateProfile(id: String, profile: Profile) extends Store[Unit]

  sealed trait Utilities[T]
  case class MergeProfile(oldProfile: Profile, newProfile: Profile) extends Utilities[Profile]
}