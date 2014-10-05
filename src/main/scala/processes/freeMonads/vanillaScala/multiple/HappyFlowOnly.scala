package processes.freeMonads.vanillaScala.multiple

import scala.concurrent.Future

import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import processes.PatchAssignment
import processes.Services
import processes.freeMonads.Id
import processes.freeMonads.multiple.HappyFlowOnlyProgramParts
import processes.freeMonads.multiple.HappyFlowOnlyProgramRunner
import processes.freeMonads.vanillaScala.MultipleMachinery

class HappyFlowOnly(protected val services: Services) extends PatchAssignment
  with MultipleMachinery with HappyFlowOnlyProgramParts with HappyFlowOnlyProgramRunner { self =>

  protected def handlePatchRequest(id: String, request: Request[AnyContent]): Future[Result] = {

    implicit val programType = ProgramType[Json +: Utilities +: Store +: Nil]

    val patchProgram =
      for {
        json <- ParseJson(request)
        newProfile <- JsonToProfile(json)
        oldProfile <- GetProfileById(id)
        mergedProfile <- MergeProfile(oldProfile, newProfile)
        _ <- UpdateProfile(id, mergedProfile)
      } yield results.noContent

    patchProgram.run(patchProgramRunner).map(_.merge)
  }

  val patchProgramRunner = {
    val storeRunner = StoreRunner
    val utilitiesRunner = UtilitiesRunner andThen IdToResultBranch andThen ResultBranchToHttpResult
    val jsonRunner = JsonRunner andThen ResultBranchToHttpResult

    storeRunner or utilitiesRunner or jsonRunner
  }

  object JsonRunner extends (Json ~> ResultBranch) {
    def apply[A](ja: Json[A]) = jsonProgramRunner(ja)
  }

  object StoreRunner extends (Store ~> HttpResult) {
    def apply[A](sa: Store[A]) = storeProgramRunner(sa)
  }

  object UtilitiesRunner extends (Utilities ~> Id) {
    def apply[A](ua: Utilities[A]) = utilitiesProgramRunner(ua)
  }

  object ResultBranchToHttpResult extends (ResultBranch ~> HttpResult) {
    def apply[A](ra: ResultBranch[A]) = resultBranchToHttpResult(ra)
  }

  object IdToResultBranch extends (Id ~> ResultBranch) {
    def apply[A](ia: Id[A]) = idToResultBranch(ia)
  }
}