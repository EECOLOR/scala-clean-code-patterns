package processes.freeMonads

import processes.Services
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.mvc.Request
import play.api.mvc.Result
import scala.concurrent.Future
import play.api.mvc.AnyContent

trait CompleteProgramRunner { _: CompleteProgramParts with HttpResultImplementation =>

  protected def services: Services

  protected def patchProgramRunner[A]: Method[A] => HttpResult[A] = {

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

  private def success[A](a: A): HttpResult[A] =
    Future successful Right(a)

  private def toRight[A, B] = Right.apply[A, B] _

  private def mapMethodResult[A, B](method: Method[A])(f: A => Either[Result, B]) =
    patchProgramRunner(method).map(_.right.flatMap(f))
}