package processes

import scala.concurrent.Future
import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.libs.json.Json.obj
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.http.Status._
import play.api.mvc.Results.BadRequest
import play.api.mvc.Results.NoContent
import play.api.mvc.Results.NotFound
import play.api.mvc.Results.UnprocessableEntity
import play.api.mvc.Results.InternalServerError
import play.api.libs.json.JsString

trait PatchAssignment {
  def patch(id: String): Action[AnyContent] =
    Action.async(handlePatchRequest(id, _: Request[AnyContent]))

  /*
   * Provided:
   * - id of the profile that is patched
   * - the http request
   * 
   * Steps:
   * - parse the request as json
   *   - if parsing of json fails -> BadRequest("expected json")
   * - validate the json against the profile structure
   *   - if json does not validate -> UnprocessableEntity(validation errors)
   * - get the existing profile by id
   *   - if no profile found by id -> NotFound(s"no profile with id $id")
   * - merge the old and the new profile
   * - update the profile at id with the merged profile
   * - return NoContent if successful
   */
  protected def handlePatchRequest(id: String, request: Request[AnyContent]): Future[Result]

  protected object results {
    val badRequest = BadRequest(error(BAD_REQUEST, "expected json"))
    
    def notFound(id: String) = NotFound(error(NOT_FOUND, s"no profile with id '$id'"))

    private type JsErrors = Seq[(JsPath, Seq[ValidationError])]
    
    val unprocessableEntity: JsErrors => Result = { errors =>
      val allErrors =
        errors.foldLeft(obj()) {
          case (errorObject, (path, errors)) =>
            errorObject ++ obj(path.toString -> errors.map(_.message))
        }
      UnprocessableEntity(error(422, allErrors))
    }
    
    val noContent = NoContent
    
    private def error(status:Int, error:JsValue):JsValue = obj("status" -> status, "error" -> error)
    private def error(status:Int, message:String):JsValue = error(status, JsString(message))
  }

  implicit val defaultExecutionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext
}