package processes.freeMonads.vanillaScala

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

object Complete extends PatchAssignment with Machinery {

  import domain.Profile

  sealed trait Method[ReturnType]
  case class ParseJson(request: Request[AnyContent]) extends Method[Option[JsValue]]
  case class JsonToProfile(json: JsValue) extends Method[JsResult[Profile]]
  case class GetProfileById(id: String) extends Method[Option[Profile]]
  case class MergeProfile(oldProfile: Profile, newProfile: Profile) extends Method[Profile]
  case class UpdateProfile(id: String, profile: Profile) extends Method[Unit]
  case class WrappedOption[A](wrapped:Method[Option[A]], ifEmpty: Result) extends Method[A]
  case class WrappedJsResult[A](
      wrapped:Method[JsResult[A]], 
      jsErrorsToResult: Seq[(JsPath, Seq[ValidationError])] => Result) extends Method[A]
  
  protected def handlePatchRequest(id: String, request: Request[AnyContent]): Future[Result] = {
    val patchProgram =
      for {
        json <- ParseJson(request) ifEmpty results.badRequest
        newProfile <- JsonToProfile(json) ifError results.unprocessableEntity
        oldProfile <- GetProfileById(id) ifEmpty results.notFound(id)
        mergedProfile <- MergeProfile(oldProfile, newProfile)
        _ <- UpdateProfile(id, mergedProfile)
      } yield results.noContent

    patchProgram.run(PatchProgramRunner)
      .map(_.merge)
      .recover(PartialFunction(results.internalServerError))
  }

  implicit class OptionMethodEnhancements[A](m:Method[Option[A]]) {
    def ifEmpty(result: Result):Method[A] = WrappedOption(m, result)
  }
  
  implicit class JsResultMethodEnhancements[A](m:Method[JsResult[A]]) {
    def ifError(jsErrorsToResult: Seq[(JsPath, Seq[ValidationError])] => Result):Method[A] = 
      WrappedJsResult(m, jsErrorsToResult)
  }
  
  object PatchProgramRunner extends (Method ~> HttpResult) {
    def apply[A](fa: Method[A]) = fa match {

      case ParseJson(request: Request[AnyContent]) =>
        success(services.parseJson(request))

      case JsonToProfile(json) =>
        success(services.jsonToProfile(json))

      case GetProfileById(id) =>
          services.getProfileById(id) map toRight

      case MergeProfile(oldProfile, newProfile) =>
        success(services.mergeProfile(oldProfile, newProfile))

      case UpdateProfile(id, profile) =>
        services.updateProfile(id, profile) map toRight
        
      case WrappedOption(method, ifEmpty) =>
        mapMethodResult(method)(_.toRight(ifEmpty))
        
      case WrappedJsResult(method, jsErrorsToResult) =>
        mapMethodResult(method) { 
          case JsSuccess(value, _) => Right(value)
          case JsError(errors) => Left(jsErrorsToResult(errors))
        }
    }
    
    private def success[A](a:A):HttpResult[A] =
      Future successful Right(a)
    
    private def toRight[A, B] = Right.apply[A, B] _
    
    private def mapMethodResult[A, B](method:Method[A])(f:A => Either[Result, B]) =
      this(method).map(_.right.flatMap(f))
  }
}