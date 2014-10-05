package processes.freeMonads

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.mvc.Result

trait HttpResultImplementation {
  implicit protected def defaultExecutionContext: ExecutionContext

  type ResultBranch[A] = Either[Result, A]
  
  object ResultBranch {
    def apply[A](a:A):ResultBranch[A] = Right(a)
    
    def flatMap[A, B](ra:ResultBranch[A])(f: A => ResultBranch[B]):ResultBranch[B] =
      ra.right.flatMap(f)
  }
  
  type HttpResult[A] = Future[ResultBranch[A]]

  object HttpResult {
    def apply[A](a:A):HttpResult[A] = this(ResultBranch(a))
    def apply[A](a:ResultBranch[A]):HttpResult[A] = Future successful a
    
    def flatMap[A, B](fa:HttpResult[A])(f: A => HttpResult[B]):HttpResult[B] =
      fa.flatMap {
        case Left(result) => Future successful Left(result)
        case Right(value) => f(value)
      }
  }
}