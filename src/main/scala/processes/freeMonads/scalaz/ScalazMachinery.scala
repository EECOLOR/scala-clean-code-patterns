package processes.freeMonads.scalaz

import scalaz.Free
import scala.language.higherKinds
import scalaz.Monad
import scala.concurrent.Future
import play.api.mvc.Result
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import processes.freeMonads.HttpResultImplementation
import scalaz.Coyoneda
import scalaz.~>

trait ScalazMachinery extends HttpResultImplementation { 

  implicit def toFree[F[_], A](fa: F[A]): Free.FreeC[F, A] =
    Free.liftFC(fa)

  type Partial[F[_]] = {
    type Free[A] = scalaz.Free.FreeC[F, A]
  }
  
  implicit def freeMonad[F[_]] = new Monad[Partial[F]#Free] {
    def point[A](a: => A):Free.FreeC[F, A] = 
      Free.point[Coyoneda.CoyonedaF[F]#A, A](a)
      
    def bind[A, B](fa: Free.FreeC[F, A])(f: A => Free.FreeC[F, B]) =
      fa.flatMap(f)
  }

  implicit val httpResultMonad = new Monad[HttpResult] {
    def point[A](a: => A) = HttpResult(a)

    def bind[A, B](fa: HttpResult[A])(f: A => HttpResult[B]) =
      HttpResult.flatMap(fa)(f)
  }
}