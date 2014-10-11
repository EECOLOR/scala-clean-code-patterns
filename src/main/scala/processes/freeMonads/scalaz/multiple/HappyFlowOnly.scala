package processes.freeMonads.scalaz.multiple

import scala.concurrent.Future
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import processes.PatchAssignment
import processes.Services
import processes.freeMonads.Id
import processes.freeMonads.multiple.HappyFlowOnlyProgramParts
import processes.freeMonads.multiple.HappyFlowOnlyProgramRunner
import processes.freeMonads.scalaz.MultipleMachinery
import play.api.libs.json.JsValue
import scalaz.Free
import scalaz.~>
import scalaz.Coproduct
import scala.language.higherKinds

class HappyFlowOnly(protected val services: Services) extends PatchAssignment
  with MultipleMachinery with HappyFlowOnlyProgramParts with HappyFlowOnlyProgramRunner { self =>

  // In order for implicit resolution of ~> we need to have an alias for all 
  // coproducts. This is because coproduct has 3 type parameters and implicit 
  // resolution for ~> only works for a single type parameters
  //
  // Note that a problem might occur when we place this inside of the method,
  // it will start complaining about diverging implicit expansion. We could 
  // place this inside of the method, but then we needed to move the program
  // parts outside of the ProgramParts trait
  type SubType0[A] = Coproduct[Utilities, Store, A]
  type ProgramType[A] = Coproduct[Json, SubType0, A]

  protected def handlePatchRequest(id: String, request: Request[AnyContent]): Future[Result] = {

    // we need to explicitly pass the program type O[_]
    def patchProgram[O[_]](implicit i1: Json ~> O,
        i2: Store ~> O,
        i3: Utilities ~> O) =
      for {
        json <- ParseJson(request)
        newProfile <- JsonToProfile(json)
        oldProfile <- GetProfileById(id)
        mergedProfile <- MergeProfile(oldProfile, newProfile)
        _ <- UpdateProfile(id, mergedProfile)
      } yield results.noContent

    // make the injectors available for the patchProgram method call
    import injectors._
    
    Free.runFC(patchProgram[ProgramType])(patchProgramRunner).map(_.merge)
  }

  val patchProgramRunner = {
    val storeRunner = StoreRunner
    val utilitiesRunner = UtilitiesRunner andThen IdToResultBranch andThen ResultBranchToHttpResult
    val jsonRunner = JsonRunner andThen ResultBranchToHttpResult

    // Trickery to help with the [_, _, _] v.s. [_] implicits
    type X[A] = Coproduct[Utilities, Store, A]
    
    val a:X ~> HttpResult = storeRunner or utilitiesRunner
    a or jsonRunner
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