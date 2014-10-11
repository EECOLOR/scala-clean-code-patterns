package processes.freeMonads.scalaz

import scala.language.higherKinds
import scala.language.implicitConversions
import scalaz.Free
import scalaz.Free.FreeC
import scalaz.~>
import scalaz.Coproduct
import scalaz._
import scalaz.Scalaz._
import scalaz.\/

trait MultipleMachinery extends SingleMachinery { self =>

  implicit def toFree[F[_], A, O[_]](fa: F[A])(
    implicit insert: F ~> O): FreeC[O, A] =
    Free.liftFC(insert(fa))

  type Co[F[_], G[_]] = {
    type Product[A] = Coproduct[F, G, A]
  }

  object injectors {
    implicit def identityTransform[F[_]] = new (F ~> F) {
      def apply[A](fa: F[A]) = fa
    }

    implicit def atCoproductHead[Head[_], Tail[_]] =
      new (Head ~> Co[Head, Tail]#Product) {
        def apply[A](fa: Head[A]) = Coproduct(fa.left)
      }

    implicit def inCoproductTail[F[_], Head[_], Tail[_]](
      implicit insertInTail: F ~> Tail) =
      new (F ~> Co[Head, Tail]#Product) {
        def apply[A](fa: F[A]) = Coproduct(insertInTail(fa).right)
      }
  }

  implicit class NaturalTransformationEnhancements[F[_], O[_]](fToO: F ~> O) {
    def or[G[_]](gToO: G ~> O) = {
      new (Co[G, F]#Product ~> O) {
        def apply[A](c: Co[G, F]#Product[A]) = c.run match {
        case -\/(g) => gToO(g)
        case \/-(f) => fToO(f)
        }
      }
    }

    def andThen[G[_]](oToG: O ~> G) =
      new (F ~> G) {
        def apply[A](fa: F[A]) = oToG(fToO(fa))
      }
  }
}