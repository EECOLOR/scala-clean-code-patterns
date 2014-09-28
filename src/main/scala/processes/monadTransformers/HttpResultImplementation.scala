package processes.monadTransformers

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.language.higherKinds

import play.api.data.validation.ValidationError
import play.api.libs.json.JsError
import play.api.libs.json.JsPath
import play.api.libs.json.JsResult
import play.api.libs.json.JsSuccess
import play.api.mvc.Result

trait HttpResultImplementation {

  type EitherT[_[_], _, _]
  type Either[+_, +_]
  
  implicit protected def defaultExecutionContext: ExecutionContext

  def Left[A](a: A): Either[A, Nothing]
  def Right[A](a: A): Either[Nothing, A]
  def EitherT[A, B](value: Future[Either[A, B]]): EitherT[Future, A, B]
  def optionToRight[A, B](o: Option[A], ifEmpty: => B): Either[B, A]

  protected type HttpResult[A] = EitherT[Future, Result, A]

  protected object HttpResult {

    def fromOption[A](ifEmpty: => Result)(o: Option[A]): HttpResult[A] =
      this(optionToEither(ifEmpty)(o))

    def fromJsResult[A](jsErrorsToResult: Seq[(JsPath, Seq[ValidationError])] => Result): JsResult[A] => HttpResult[A] = {
      case JsSuccess(value, _) =>
        this(Right(value))
      case JsError(errors) =>
        this(Left(jsErrorsToResult(errors)))
    }

    def fromFutureOption[A](ifEmpty: => Result)(f: Future[Option[A]]): HttpResult[A] =
      this(f map optionToEither(ifEmpty))

    def fromAnyFuture[A](f: Future[A]): HttpResult[A] =
      this(f map Right)

    def fromAny[A](a: A): HttpResult[A] =
      this(Right(a))

    private def apply[A](f: Future[Either[Result, A]]): HttpResult[A] = EitherT(f)

    private def apply[A](e: Either[Result, A]): HttpResult[A] = this(Future successful e)

    private def optionToEither[A](ifEmpty: => Result): Option[A] => Either[Result, A] =
      optionToRight(_, ifEmpty)
  }
}