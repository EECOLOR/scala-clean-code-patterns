package processes.monadTransformers.scalaz

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.language.higherKinds

import processes.monadTransformers.HttpResultImplementation
import scalaz.-\/
import scalaz.Scalaz.ToOptionOpsFromOption
import scalaz.\/
import scalaz.\/-

trait ScalazMachinery extends HttpResultImplementation {
  
  type EitherT[F[_], A, B] = scalaz.EitherT[F, A, B]
  type Either[+A, +B] = A \/ B

  def Left[A](a: A) = -\/(a)
  def Right[A](a: A) = \/-(a)
  def EitherT[A, B](value: Future[A \/ B]) = scalaz.EitherT(value)
  def optionToRight[A, B](o: Option[A], ifEmpty: => B) = o \/> ifEmpty

  implicit protected def ToIdOps[A] = scalaz.Scalaz.ToIdOps[A] _
  implicit protected def futureInstance = scalaz.Scalaz.futureInstance(defaultExecutionContext)
}