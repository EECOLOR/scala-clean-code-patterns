package processes.freeMonads

import domain.Profile
import play.api.libs.json.JsValue
import play.api.mvc.Request
import play.api.mvc.AnyContent

trait HappyFlowOnlyProgramParts {

  sealed trait Method[ReturnType]
  case class ParseJson(request: Request[AnyContent]) extends Method[JsValue]
  case class JsonToProfile(json: JsValue) extends Method[Profile]
  case class GetProfileById(id: String) extends Method[Profile]
  case class MergeProfile(oldProfile: Profile, newProfile: Profile) extends Method[Profile]
  case class UpdateProfile(id: String, profile: Profile) extends Method[Unit]
  
}