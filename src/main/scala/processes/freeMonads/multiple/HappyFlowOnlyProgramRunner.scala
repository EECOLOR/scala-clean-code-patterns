package processes.freeMonads.multiple

import play.api.mvc.Request
import scala.concurrent.Future
import play.api.mvc.AnyContent
import processes.Services
import processes.PatchAssignment
import processes.freeMonads.Id
import processes.freeMonads.HttpResultImplementation
import scala.Right

trait HappyFlowOnlyProgramRunner { _: HttpResultImplementation with HappyFlowOnlyProgramParts with PatchAssignment =>

  protected def services: Services

  protected def jsonProgramRunner[A]: Json[A] => ResultBranch[A] = {
    case ParseJson(request: Request[AnyContent]) =>
      services
        .parseJson(request)
        .toRight(left = results.badRequest)

    case JsonToProfile(json) =>
      services
        .jsonToProfile(json)
        .asEither
        .left.map(results.unprocessableEntity)
  }

  protected def storeProgramRunner[A]: Store[A] => HttpResult[A] = {
    case GetProfileById(id) =>
      services
        .getProfileById(id)
        .map(_.toRight(left = results.notFound(id)))

    case UpdateProfile(id, profile) =>
      services
        .updateProfile(id, profile)
        .map(Right.apply)
  }

  protected def utilitiesProgramRunner[A]: Utilities[A] => Id[A] = {
    case MergeProfile(oldProfile, newProfile) =>
      services.mergeProfile(oldProfile, newProfile)
  }

  protected def idToResultBranch[A]: Id[A] => ResultBranch[A] = 
    ResultBranch(_)
  
  protected def resultBranchToHttpResult[A]: ResultBranch[A] => HttpResult[A] =
    HttpResult(_) 
}