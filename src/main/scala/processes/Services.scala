package processes

import domain.Profile
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.mvc.AnyContent

trait Services {
  def parseJson(request: Request[AnyContent]): Option[JsValue]

  def jsonToProfile(json: JsValue): JsResult[Profile]

  def getProfileById(id: String): Future[Option[Profile]]

  def mergeProfile(oldProfile: Profile, newProfile: Profile): Profile

  def updateProfile(id: String, profile: Profile): Future[Unit]
}