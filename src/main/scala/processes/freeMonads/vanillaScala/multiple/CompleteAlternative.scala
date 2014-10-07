package processes.freeMonads.vanillaScala.multiple

import processes.Services
import play.api.mvc.Result
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.mvc.AnyContent
import processes.freeMonads.Id

class CompleteAlternative(services: Services) extends Complete(services) with ProgramMerger {
  
  override protected def handlePatchRequest(id: String, request: Request[AnyContent]): Future[Result] = {

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

    /*
     * An alternative (requires more complicated code behind the screens) that
     * merges any branch that has the same type as the program result.
     * 
     * So in our case it goes from
     *   Json +: Store +: Utilities +: Http +: Branch[Result]#Instance
     * to
     *   Json +: Store +: Utilities +: Http
     *   
     * Allowing a more simple version of the runner
     */
    patchProgram.mergeBranch.run(alternativePathProgramRunner)
  }
  
  val alternativePathProgramRunner = {
    val idToFuture = new (Id ~> Future) {
      def apply[A](ia: Id[A]) = Future successful ia
    }

    val storeRunner = StoreRunner
    val utilitiesRunner = UtilitiesRunner andThen idToFuture
    val jsonRunner = JsonRunner andThen idToFuture
    val httpRunner = HttpRunner andThen idToFuture

    httpRunner or utilitiesRunner or storeRunner or jsonRunner
  }
  
  // The program no longer has HttpResult[A] (Future[Either[Result, A]]) because 
  // the branches were merged. We now require a monad for Future to run the 
  // program.
  implicit val futureMonad = new Monad[Future] {
    def create[A](a: A) = Future successful a
    def flatMap[A, B](fa: Future[A])(f: A => Future[B]) =
      fa.flatMap(f)
  }
}