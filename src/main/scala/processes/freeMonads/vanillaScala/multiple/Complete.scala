package processes.freeMonads.vanillaScala.multiple

import scala.concurrent.Future
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import processes.PatchAssignment
import processes.Services
import processes.freeMonads.multiple.CompleteProgramParts
import processes.freeMonads.Id
import processes.freeMonads.multiple.CompleteProgramRunner
import processes.freeMonads.vanillaScala.MultipleMachinery
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.libs.json.JsResult
import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath
import scala.language.higherKinds

class Complete(protected val services: Services) extends PatchAssignment
  with MultipleMachinery with CompleteProgramParts with CompleteProgramRunner
  with BranchingUtils {

  protected def handlePatchRequest(id: String, request: Request[AnyContent]): Future[Result] = {

    implicit val programType =
      ProgramType[Json +: Store +: Utilities +: Http +: Branch[Result]#Instance +: Nil]

    val patchProgram =
      for {
        json <- ParseJson(request) ifEmpty BadRequest
        newProfile <- JsonToProfile(json) ifError ValidationErrors
        oldProfile <- GetProfileById(id) ifEmpty NotFound(id)
        mergedProfile <- MergeProfile(oldProfile, newProfile)
        _ <- UpdateProfile(id, mergedProfile)
      } yield results.noContent

    patchProgram.run(patchProgramRunner).map(_.merge)
  }

  val patchProgramRunner = {
    val idToHttpResult = IdToResultBranch andThen ResultBranchToHttpResult

    val storeRunner = StoreRunner andThen FutureToHttpResult
    val utilitiesRunner = UtilitiesRunner andThen idToHttpResult
    val jsonRunner = JsonRunner andThen idToHttpResult
    val httpRunner = HttpRunner andThen idToHttpResult
    val branchRunner = ResultBranchRunner andThen ResultBranchToHttpResult

    branchRunner or httpRunner or utilitiesRunner or storeRunner or jsonRunner
  }

  object JsonRunner extends (Json ~> Id) {
    def apply[A](ja: Json[A]) = jsonProgramRunner(ja)
  }

  object StoreRunner extends (Store ~> Future) {
    def apply[A](sa: Store[A]) = storeProgramRunner(sa)
  }

  object UtilitiesRunner extends (Utilities ~> Id) {
    def apply[A](ua: Utilities[A]) = utilitiesProgramRunner(ua)
  }

  object HttpRunner extends (Http ~> Id) {
    def apply[A](ha: Http[A]) = httpProgramRunner(ha)
  }

  object ResultBranchRunner extends (Branch[Result]#Instance ~> ResultBranch) {
    def apply[A](ba: Branch[Result]#Instance[A]) = resultBranchProgramRunner(ba)
  }

  object ResultBranchToHttpResult extends (ResultBranch ~> HttpResult) {
    def apply[A](ra: ResultBranch[A]) = resultBranchToHttpResult(ra)
  }

  object IdToResultBranch extends (Id ~> ResultBranch) {
    def apply[A](ia: Id[A]) = idToResultBranch(ia)
  }

  object FutureToHttpResult extends (Future ~> HttpResult) {
    def apply[A](fa: Future[A]) = futureToHttpResult(fa)
  }

  // Check the createBranch method for an explanation
  implicit class OptionMethodEnhancements[F[_], R](right: F[Option[R]]) {

    def ifEmpty[G[_], L](left: G[L])(
      implicit brancher: Brancher[F, G, L]) =

      // When the option is empty, use the result of left
      brancher.branch(right)(_.toRight(left))
  }

  // Check the createBranch method for an explanation
  implicit class JsResultMethodEnhancements[F[_], R](right: F[JsResult[R]]) {

    def ifError[G[_], L](jsErrorsToResult: Seq[(JsPath, Seq[ValidationError])] => G[L])(
      implicit brancher: Brancher[F, G, L]) =

      // When the JsResult is a success return that value, otherwise use the 
      // contents of the error to create the left value and use it's result
      brancher.branch(right)(_.asEither.left.map(jsErrorsToResult))
  }
}
