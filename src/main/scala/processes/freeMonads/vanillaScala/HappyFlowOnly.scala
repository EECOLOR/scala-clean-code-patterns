package processes.freeMonads.vanillaScala

import processes.PatchAssignment
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.mvc.Request
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.mvc.AnyContent

object HappyFlowOnly extends PatchAssignment with Machinery {

  import domain.Profile

  sealed trait Method[ReturnType]
  case class ParseJson(request: Request[AnyContent]) extends Method[JsValue]
  case class JsonToProfile(json: JsValue) extends Method[Profile]
  case class GetProfileById(id: String) extends Method[Profile]
  case class MergeProfile(oldProfile: Profile, newProfile: Profile) extends Method[Profile]
  case class UpdateProfile(id: String, profile: Profile) extends Method[Unit]
  
  protected def handlePatchRequest(id: String, request: Request[AnyContent]): Future[Result] = {
    val patchProgram =
      for {
        json <- ParseJson(request)
        newProfile <- JsonToProfile(json)
        oldProfile <- GetProfileById(id)
        mergedProfile <- MergeProfile(oldProfile, newProfile)
        _ <- UpdateProfile(id, mergedProfile)
      } yield results.noContent

    patchProgram.run(PatchProgramRunner)
      .map(_.merge)
      .recover(PartialFunction(results.internalServerError))
  }

  type BranchedResult[A] = Future[Either[Result, A]]

  object PatchProgramRunner extends (Method ~> BranchedResult) {
    def apply[A](fa: Method[A]) = fa match {

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
  
  implicit val branchedResultMonad = new Monad[BranchedResult] {
    def create[A](a: A) = Future successful Right(a)

    def flatMap[A, B](fa: BranchedResult[A])(f: A => BranchedResult[B]) =
      fa.flatMap {
        case Left(result) => Future successful Left(result)
        case Right(value) => f(value)
      }
  }
}