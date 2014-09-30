package processes.freeMonads.vanillaScala

import scala.concurrent.Future

import domain.Profile
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import processes.PatchAssignment
import processes.Services
import processes.freeMonads.NestedProgramParts
import processes.freeMonads.NestedProgramRunner

class Nested(protected val services: Services) extends PatchAssignment
  with Machinery with NestedProgramRunner with NestedProgramParts {

  protected def handlePatchRequest(id: String, request: Request[AnyContent]): Future[Result] = {
    val patchProgram =
      for {
        profile <- RequestToInternalRepresentation(request)
        serviceResult <- PerformProfileUpdate(id, profile)
        response <- InternalRepresentationToResponse(serviceResult)
      } yield response

    val serviceProgram = patchProgram.run(PatchProgramRunner)
    serviceProgram.run(ServiceRunner).map(_.merge)
  }

  sealed trait SubRoutine[T]
  case class RequestToInternalRepresentation(request: Request[AnyContent]) extends SubRoutine[Profile] {
    val program =
      for {
        json <- ParseJson(request) ifEmpty BadRequest
        profile <- JsonToProfile(json) ifError ValidationErrors
      } yield profile
  }

  case class PerformProfileUpdate(id: String, profile: Profile) extends SubRoutine[ServiceResult] {
    val program =
      for {
        oldProfile <- GetProfileById(id) ifEmpty NotFound(id)
        mergedProfile <- MergeProfile(oldProfile, profile)
        _ <- UpdateProfile(id, mergedProfile)
      } yield Success: ServiceResult
  }

  case class InternalRepresentationToResponse(serviceResult: ServiceResult) extends SubRoutine[Result] {
    val result =
      serviceResult match {
        case Success => results.noContent
        case BadRequest => results.badRequest
        case ValidationErrors(errors) => results.unprocessableEntity(errors)
        case NotFound(id) => results.notFound(id)
      }
  }

  type Routine[A] = Free[Service, A]

  protected def serviceResultToResponse(serviceResult: ServiceResult): Result =
    InternalRepresentationToResponse(serviceResult).result

  object PatchProgramRunner extends (SubRoutine ~> Routine) {
    def apply[A](sa: SubRoutine[A]) = sa match {
      case x @ RequestToInternalRepresentation(_) => x.program
      case x @ PerformProfileUpdate(_, _) => x.program
      case x @ InternalRepresentationToResponse(_) => Return(x.result)
    }
  }
  
  object ServiceRunner extends (Service ~> HttpResult) {
    def apply[A](sa: Service[A]) = serviceRunner(sa)
  }
}