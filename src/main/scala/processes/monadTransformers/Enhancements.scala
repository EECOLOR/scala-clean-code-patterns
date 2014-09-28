package processes.monadTransformers

import play.api.libs.json.JsResult
import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath
import play.api.mvc.Result
import scala.concurrent.Future

trait Enhancements { _: HttpResultImplementation =>

  implicit class OptionEnhancements[A](o: Option[A]) {
    def ifEmpty(result: Result) = HttpResult.fromOption(result)(o)
  }

  implicit class JsResultEnhancements[A](r: JsResult[A]) {
    def ifError(jsErrorsToResult: Seq[(JsPath, Seq[ValidationError])] => Result) =
      HttpResult.fromJsResult(jsErrorsToResult)(r)
  }

  implicit class FutureOptionEnhancements[A](f: Future[Option[A]]) {
    def ifEmpty(result: Result) = HttpResult.fromFutureOption(ifEmpty = result)(f)
  }

  implicit class AnyEnhancement[A](a: A) {
    def toResult = HttpResult.fromAny(a)
  }
  implicit class AnyFutureEnhancement[A](f: Future[A]) {
    def toResult = HttpResult.fromAnyFuture(f)
  }
}