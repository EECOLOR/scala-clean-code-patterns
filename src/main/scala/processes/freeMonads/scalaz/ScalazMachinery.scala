package processes.freeMonads.scalaz

import scalaz.Free
import scala.language.higherKinds
import scalaz.Monad
import scala.concurrent.Future
import play.api.mvc.Result
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import processes.freeMonads.HttpResultImplementation

trait ScalazMachinery extends HttpResultImplementation {

  implicit def toFree[F[_], A](fa: F[A]): Free.FreeC[F, A] =
    Free.liftFC(fa)

  implicit val httpResultMonad = new Monad[HttpResult] {
    def point[A](a: => A) = HttpResult(a)

    def bind[A, B](fa: HttpResult[A])(f: A => HttpResult[B]) =
      HttpResult.flatMap(fa)(f)
  }
}