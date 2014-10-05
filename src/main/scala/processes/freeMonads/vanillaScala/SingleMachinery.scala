package processes.freeMonads.vanillaScala

import scala.language.higherKinds
import scala.language.implicitConversions
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.mvc.Result
import processes.freeMonads.HttpResultImplementation

trait SingleMachinery extends HttpResultImplementation { self =>

  implicit protected def defaultExecutionContext: ExecutionContext

  implicit def toFree[F[_], A](fa: F[A]): Free[F, A] =
    FlatMap(fa, (a: A) => Create(a))

  trait ~>[-F[_], +G[_]] {
    def apply[A](fa: F[A]): G[A]
  }

  sealed trait Free[F[_], A] {
    def flatMap[B](f: A => Free[F, B]): Free[F, B] =
      this match {
        case Create(value) => f(value)
        case FlatMap(fa, g) => FlatMap(fa, g andThen (_ flatMap f))
      }

    def map[B](f: A => B): Free[F, B] =
      flatMap((a: A) => Create(f(a)))

    def run[G[_]](runner: F ~> G)(implicit G: Monad[G]): G[A] =
      this match {
        case Create(value) => G.create(value)
        case FlatMap(fa, f) =>
          val ga = runner(fa)
          G.flatMap(ga)(f andThen (_ run runner))
      }
  }

  case class Create[F[_], A](value: A) extends Free[F, A]

  case class FlatMap[F[_], A, B](fa: F[A], f: A => Free[F, B]) extends Free[F, B]

  trait Monad[F[_]] {
    def create[A](a: A): F[A]
    def flatMap[A, B](fa: F[A])(f: A => F[B]): F[B]
  }

  type Partial[F[_]] = {
    type Free[A] = self.Free[F, A]
  }
  implicit def freeMonad[F[_]] = new Monad[Partial[F]#Free] {
    def create[A](a: A) = Create(a)
    def flatMap[A, B](fa: Free[F, A])(f: A => Free[F, B]) =
      fa.flatMap(f)
  }

  implicit val httpResultMonad = new Monad[HttpResult] {
    def create[A](a: A) = HttpResult(a)

    def flatMap[A, B](fa: HttpResult[A])(f: A => HttpResult[B]) =
      HttpResult.flatMap(fa)(f)
  }
}