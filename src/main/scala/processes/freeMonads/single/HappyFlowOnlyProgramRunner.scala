package processes.freeMonads.single

import play.api.mvc.Request
import scala.concurrent.Future
import play.api.mvc.AnyContent
import processes.Services
import processes.PatchAssignment
import processes.freeMonads.HttpResultImplementation
import scala.Right

trait HappyFlowOnlyProgramRunner { _:HttpResultImplementation with HappyFlowOnlyProgramParts with PatchAssignment =>

  protected def services: Services

  protected def patchProgramRunner[A]: Method[A] => HttpResult[A] = {
    case ParseJson(request: Request[AnyContent]) =>
      val result =
        services
          .parseJson(request)
          .toRight(left = results.badRequest)

      Future successful result

    case JsonToProfile(json) =>
      val result =
        services
          .jsonToProfile(json)
          .asEither
          .left.map(results.unprocessableEntity)

      Future successful result

    case GetProfileById(id) =>
      services
        .getProfileById(id)
        .map(_.toRight(left = results.notFound(id)))

    case MergeProfile(oldProfile, newProfile) =>
      val result = services.mergeProfile(oldProfile, newProfile)

      Future successful Right(result)

    case UpdateProfile(id, profile) =>
      services
        .updateProfile(id, profile)
        .map(Right.apply)
  }
}