package processes.freeMonads.multiple

import processes.Services
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.mvc.Request
import play.api.mvc.Result
import scala.concurrent.Future
import play.api.mvc.AnyContent
import processes.freeMonads.HttpResultImplementation
import processes.freeMonads.Id
import processes.PatchAssignment

trait CompleteProgramRunner { _: CompleteProgramParts with HttpResultImplementation with PatchAssignment =>

  protected def services: Services

  protected def jsonProgramRunner[A]: Json[A] => Id[A] = {
    case ParseJson(request: Request[AnyContent]) =>
      services.parseJson(request)

    case JsonToProfile(json) =>
      services.jsonToProfile(json)
  }

  protected def storeProgramRunner[A]: Store[A] => Future[A] = {
    case GetProfileById(id) =>
      services.getProfileById(id)

    case UpdateProfile(id, profile) =>
      services.updateProfile(id, profile)
  }

  protected def utilitiesProgramRunner[A]: Utilities[A] => Id[A] = {
    case MergeProfile(oldProfile, newProfile) =>
      services.mergeProfile(oldProfile, newProfile)
  }

  protected def httpProgramRunner[A]: Http[A] => Id[A] = {
    case BadRequest => results.badRequest
    case ValidationErrors(errors) => results.unprocessableEntity(errors)
    case NotFound(id: String) => results.notFound(id)
  }

  protected def resultBranchProgramRunner[A]: Branch[Result]#Instance[A] => ResultBranch[A] = {
    branch => Left(branch.value)
  }

  protected def idToResultBranch[A]: Id[A] => ResultBranch[A] =
    ResultBranch(_)

  protected def resultBranchToHttpResult[A]: ResultBranch[A] => HttpResult[A] =
    HttpResult(_)

  protected def futureToHttpResult[A]: Future[A] => HttpResult[A] =
    _ map Right.apply
}