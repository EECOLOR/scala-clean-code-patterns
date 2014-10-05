package processes.freeMonads.vanillaScala.single

import scala.concurrent.Future
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import processes.PatchAssignment
import processes.Services
import processes.freeMonads.single.CompleteProgramParts
import processes.freeMonads.single.CompleteProgramRunner
import processes.freeMonads.vanillaScala.SingleMachinery

class Complete(protected val services: Services) extends PatchAssignment
  with SingleMachinery with CompleteProgramParts with CompleteProgramRunner {

  protected def handlePatchRequest(id: String, request: Request[AnyContent]): Future[Result] = {
    val patchProgram =
      for {
        json <- ParseJson(request) ifEmpty results.badRequest
        newProfile <- JsonToProfile(json) ifError results.unprocessableEntity
        oldProfile <- GetProfileById(id) ifEmpty results.notFound(id)
        mergedProfile <- MergeProfile(oldProfile, newProfile)
        _ <- UpdateProfile(id, mergedProfile)
      } yield results.noContent

    patchProgram.run(PatchProgramRunner).map(_.merge)
  }
  
  object PatchProgramRunner extends (Method ~> HttpResult) {
    def apply[A](fa: Method[A]) = patchProgramRunner(fa)    
  }
}