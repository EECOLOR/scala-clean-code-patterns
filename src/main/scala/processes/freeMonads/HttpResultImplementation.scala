package processes.freeMonads

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.mvc.Result

trait HttpResultImplementation {
  implicit protected def defaultExecutionContext: ExecutionContext

  type HttpResult[A] = Future[Either[Result, A]]

  object HttpResult {
    def apply[A](a:A):HttpResult[A] = Future successful Right(a)
    
    def flatMap[A, B](fa:HttpResult[A])(f: A => HttpResult[B]):HttpResult[B] =
      fa.flatMap {
        case Left(result) => Future successful Left(result)
        case Right(value) => f(value)
      }
  }
}