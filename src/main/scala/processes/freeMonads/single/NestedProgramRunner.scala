package processes.freeMonads.single

import processes.Services
import play.api.libs.json.JsSuccess
import play.api.libs.json.JsError
import play.api.mvc.Request
import play.api.mvc.Result
import scala.concurrent.Future
import play.api.mvc.AnyContent
import scala.Left
import scala.Right
import processes.freeMonads.HttpResultImplementation

trait NestedProgramRunner { _: NestedProgramParts with HttpResultImplementation =>

  protected def services: Services

  protected def serviceResultToResponse(s:ServiceResult):Result
  
  protected def serviceRunner[A]: Service[A] => HttpResult[A] = {

    case Return(result) => 
      success(result)
    
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
      mapMethodResult(method)(_.toRight(serviceResultToResponse(ifEmpty)))

    case WrappedJsResult(method, jsErrorsToResult) =>
      mapMethodResult(method) {
        case JsSuccess(value, _) => Right(value)
        case JsError(errors) =>
          Left(serviceResultToResponse(jsErrorsToResult(errors)))
      }
  }

  private def success[A](a: A): HttpResult[A] =
    Future successful Right(a)

  private def toRight[A, B] = Right.apply[A, B] _

  private def mapMethodResult[A, B](method: Service[A])(f: A => Either[Result, B]) =
    serviceRunner(method).map(_.right.flatMap(f))
}