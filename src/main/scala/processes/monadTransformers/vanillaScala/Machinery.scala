package processes.monadTransformers.vanillaScala

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.language.higherKinds

import processes.monadTransformers.HttpResultImplementation

trait Machinery extends HttpResultImplementation {
  
  type EitherT[F[_], A, B] = Machinery.EitherT[F, A, B]
  type Either[+A, +B] = scala.Either[A, B]
  
  def Left[A](a: A) = scala.Left(a)
  def Right[A](a: A) = scala.Right(a)
  def EitherT[A, B](value: Future[Either[A, B]]) = Machinery.EitherT(value)
  def optionToRight[A, B](o: Option[A], ifEmpty: => B) = o.toRight(left = ifEmpty)
  
  implicit protected def defaultExecutionContext: ExecutionContext

  implicit class IdOps[A](self: A) {
    final def |>[B](f: A => B): B = f(self)
  }
}

object Machinery {
  trait Functor[F[_]] {
    def map[A, B](fa: F[A])(f: A => B): F[B]
  }
  object Functor {
    class MonadBasedFunctor[F[_]](F: Monad[F]) extends Functor[F] {
      def map[A, B](fa: F[A])(f: A => B): F[B] =
        F.flatMap(fa)(a => F.create(f(a)))
    }

    implicit def forMonad[F[_]](implicit F: Monad[F]): Functor[F] =
      new MonadBasedFunctor(F)
  }

  trait Monad[F[_]] {
    def create[A](a: A): F[A]
    def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  }

  implicit def futureMonad(implicit ec: ExecutionContext): Monad[Future] =
    new Monad[Future] {
      def create[A](a: A): Future[A] = Future successful a
      def flatMap[A, B](fa: Future[A])(f: A => Future[B]): Future[B] = fa flatMap f
    }

  case class EitherT[F[_], A, B](run: F[Either[A, B]]) {

    def flatMap[C](f: B => EitherT[F, A, C])(implicit F: Monad[F]): EitherT[F, A, C] =
      EitherT(F.flatMap(run)(_.fold(a => F.create(Left(a): Either[A, C]), f(_).run)))

    def map[C](f: B => C)(implicit F: Functor[F]): EitherT[F, A, C] =
      EitherT(F.map(run)(_.right.map(f)))
  }
}