package test

import scala.concurrent.Future

import play.api.libs.json.JsValue
import play.api.mvc.AnyContent
import play.api.mvc.Request
import processes.PatchAssignment

object RegularScala extends PatchAssignment {

  import domain.Profile

  def handlePatchRequest(id: String, request: Request[AnyContent]) =
    services.parseJson(request)
      .map(convertToProfileAndUpdate(id))
      .getOrElse(toFuture(results.badRequest))
      .recover(PartialFunction(results.internalServerError))

  private def convertToProfileAndUpdate(id: String)(json: JsValue) =
    services.jsonToProfile(json)
      .asEither
      .right.map(performProfileUpdate(id))
      .left.map(results.unprocessableEntity andThen toFuture)
      .merge

  private def performProfileUpdate(id: String)(newProfile: Profile) =
    services.getProfileById(id)
      .flatMap {
        case Some(oldProfile) => mergeAndUpdateProfile(id, newProfile, oldProfile)
        case None => profileNotFound(id)
      }

  private def mergeAndUpdateProfile(id: String, newProfile: Profile, oldProfile: Profile) = {
    val mergedProfile = services.mergeProfile(oldProfile, newProfile)
    services.updateProfile(id, mergedProfile)
      .map(_ => results.noContent)
  }

  private def profileNotFound(id: String) = toFuture(results.notFound(id))

  private def toFuture[T](t: T) = Future successful t
}