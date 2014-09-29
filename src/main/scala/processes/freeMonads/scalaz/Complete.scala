package processes.freeMonads.scalaz

import processes.PatchAssignment
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.mvc.AnyContent
import play.api.libs.json.JsPath
import play.api.data.validation.ValidationError
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import processes.Services
import processes.freeMonads.CompleteProgramParts
import scalaz.Coyoneda
import scalaz.~>
import processes.freeMonads.CompleteProgramRunner

class Complete(protected val services: Services) extends PatchAssignment
  with ScalazMachinery with CompleteProgramParts with CompleteProgramRunner {

  protected def handlePatchRequest(id: String, request: Request[AnyContent]): Future[Result] = {
    val patchProgram =
      for {
        json <- ParseJson(request) ifEmpty results.badRequest
        newProfile <- JsonToProfile(json) ifError results.unprocessableEntity
        oldProfile <- GetProfileById(id) ifEmpty results.notFound(id)
        mergedProfile <- MergeProfile(oldProfile, newProfile)
        _ <- UpdateProfile(id, mergedProfile)
      } yield results.noContent

    patchProgram.foldMap(Coyoneda.liftTF(PatchProgramRunner)).map(_.merge)
  }

  object PatchProgramRunner extends (Method ~> HttpResult) {
    def apply[A](fa: Method[A]) = patchProgramRunner(fa)
  }
}