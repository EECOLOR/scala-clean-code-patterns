package processes

import org.qirx.littlespec.Specification
import domain.Profile
import play.api.libs.json.JsResult
import play.api.libs.json.JsValue
import play.api.mvc.Request
import scala.concurrent.Future
import scala.collection.mutable
import play.api.mvc.AnyContent
import play.api.libs.json.JsSuccess
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.libs.json.JsString
import play.api.libs.json.Json.obj
import play.api.libs.json.Json.arr
import play.api.test.Helpers._
import play.api.mvc.Result
import play.api.libs.json.Reads
import play.api.libs.json.JsObject
import play.api.mvc.AnyContentAsEmpty
import play.api.test.Helpers
import play.api.libs.concurrent.Execution.Implicits.defaultContext

abstract class PatchAssignmentSpec(
  name: String,
  assignment: Services => PatchAssignment) extends Specification { self =>

  name - {

    "Report bad request when receiving an invalid json request" - {
      val result = request(_.withTextBody("Not a valid json request"))

      result.isError(400, "expected json")
    }

    "Report validation error for invalid structure" - {
      val result = request(_.withJsonBody(JsString("Not a valid json structure")))

      val error = obj("" -> arr("error.expected.jsobject"))

      result.isError(422, error)
    }

    "Report validation error for invalid values" - {
      val result = request(_.withJsonBody(obj("name" -> 10, "email" -> true)))

      val error = obj(
        "/email" -> arr("error.expected.jsstring"),
        "/name" -> arr("error.expected.jsstring")
      )

      result.isError(422, error)
    }

    "Report not found error for invalid id" - {
      val result = request("non-existent", _.withJsonBody(obj()))

      result.isError(404, "no profile with id 'non-existent'")
    }

    "No changes if the object is empty" - {
      val testProfile = Profile(Some("testEmail"), Some("testName"))
      val testServices = new TestServices(mutable.Map("id" -> testProfile))

      val result = request(testServices, "id", _.withJsonBody(obj()))

      status(result) is 204
      testServices.profiles("id") is testProfile
    }

    "Change the correct field(s)" - {
      val testProfile = Profile(Some("testEmail"), Some("testName"))
      def testServices = new TestServices(mutable.Map("id" -> testProfile))

      val testServices1 = testServices
      val result1 = request(testServices1, "id", _.withJsonBody(obj("email" -> "newEmail")))

      status(result1) is 204
      testServices1.profiles("id") is Profile(Some("newEmail"), Some("testName"))

      val testServices2 = testServices
      val result2 = request(testServices2, "id", _.withJsonBody(obj("name" -> "newName")))

      status(result2) is 204
      testServices2.profiles("id") is Profile(Some("testEmail"), Some("newName"))

      val testServices3 = testServices
      val result3 = request(testServices3, "id", _.withJsonBody(obj("name" -> "newName", "email" -> "newEmail")))

      status(result3) is 204
      testServices3.profiles("id") is Profile(Some("newEmail"), Some("newName"))
    }

    "Add the correct field(s)" - {
      val testProfile = Profile(None, None)
      def testServices = new TestServices(mutable.Map("id" -> testProfile))

      val testServices1 = testServices
      val result1 = request(testServices1, "id", _.withJsonBody(obj("email" -> "newEmail")))

      status(result1) is 204
      testServices1.profiles("id") is Profile(Some("newEmail"), None)

      val testServices2 = testServices
      val result2 = request(testServices2, "id", _.withJsonBody(obj("name" -> "newName")))

      status(result2) is 204
      testServices2.profiles("id") is Profile(None, Some("newName"))

      val testServices3 = testServices
      val result3 = request(testServices3, "id", _.withJsonBody(obj("name" -> "newName", "email" -> "newEmail")))

      status(result3) is 204
      testServices3.profiles("id") is Profile(Some("newEmail"), Some("newName"))
    }
  }

  lazy val emptyProfile = Profile(None, None)

  class TestServices(
    val profiles: mutable.Map[String, Profile] = mutable.Map.empty)
    extends Services {

    def parseJson(request: Request[AnyContent]): Option[JsValue] =
      request.body.asJson

    def jsonToProfile(json: JsValue): JsResult[Profile] = {
      val obj = implicitly[Reads[JsObject]] reads json
      obj.flatMap(profileFormat.reads)
    }

    def getProfileById(id: String): Future[Option[Profile]] =
      inTheFuture {
        profiles.get(id)
      }

    def mergeProfile(oldProfile: Profile, newProfile: Profile): Profile =
      profileFormat
        .reads {
          (profileFormat writes oldProfile) ++ (profileFormat writes newProfile)
        }
        .get

    def updateProfile(id: String, profile: Profile): Future[Unit] =
      inTheFuture {
        profiles += id -> profile
        unit
      }

    private def inTheFuture[T](code: => T) =
      Future {
        Thread.sleep(10)
        code
      }

    private lazy val profileFormat = Json.format[Profile]
    private lazy val unit = ()
  }

  lazy val defaultTestServices = new TestServices

  def request[A <: AnyContent](services: Services, id: String, f: FakeRequest[AnyContentAsEmpty.type] => Request[A]): Future[Result] =
    assignment(services)
      .patch(id)
      .apply(f(FakeRequest()))

  def request[A <: AnyContent](id: String, f: FakeRequest[AnyContentAsEmpty.type] => Request[A]): Future[Result] =
    request(defaultTestServices, id, f)

  def request[A <: AnyContent](f: FakeRequest[AnyContentAsEmpty.type] => Request[A]): Future[Result] =
    request("", f)

  implicit class ResultEnhancements(result: Future[Result]) {

    def isError(status: Int, error: String): FragmentBody =
      isError(status, JsString(error))

    def isError(status: Int, error: JsValue): FragmentBody = {
      Helpers.contentAsJson(result) is obj(
        "status" -> status,
        "error" -> error
      )
      Helpers.status(result) is status
    }
  }
}