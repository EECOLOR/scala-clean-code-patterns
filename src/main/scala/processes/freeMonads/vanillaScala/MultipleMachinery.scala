package processes.freeMonads.vanillaScala

import scala.language.higherKinds
import scala.language.implicitConversions

trait MultipleMachinery extends SingleMachinery { self =>

  implicit def toFree[F[_], A, O[_]](fa: F[A])(
    implicit programType: ProgramType[O],
    insert: F ~> O): Free[O, A] =
    Free.lift(insert(fa))

  trait +:[F[_], T]
  trait Nil

  trait ProgramType[F[_]]
  object ProgramType {
    def apply[T](implicit to: ToParameterized[T]): ProgramType[to.Out] = null
  }

  trait ToParameterized[T] {
    type Out[_]
  }

  object ToParameterized {
    implicit def withEmpty[F[_]]: ToParameterized[F +: Nil] {
      type Out[A] = F[A]
    } = null

    implicit def typeSet[F[_], G[_], X](
      implicit to: ToParameterized[G +: X]): ToParameterized[F +: G +: X] {
      type Out[A] = Co[F, to.Out]#Product[A]
    } = null
  }

  // defining coproduct like this helps with implicit resolution
  class Co[F[_], G[_]] {
    case class Product[A](value: Either[F[A], G[A]])
  }
  object Co {
    def apply[F[_], G[_]] = new Co[F, G]
  }

  implicit def identityTransform[F[_]] = new (F ~> F) {
    def apply[A](fa: F[A]) = fa
  }

  implicit def atCoproductHead[Head[_], Tail[_]] =
    new (Head ~> Co[Head, Tail]#Product) {
      def apply[A](fa: Head[A]) = Co[Head, Tail].Product(Left(fa))
    }

  implicit def inCoproductTail[F[_], Head[_], Tail[_]](
    implicit insertInTail: F ~> Tail) =
    new (F ~> Co[Head, Tail]#Product) {
      def apply[A](fa: F[A]) = Co[Head, Tail].Product(Right(insertInTail(fa)))
    }

  implicit class NaturalTransformationEnhancements[F[_], O[_]](fToO: F ~> O) {
    def or[G[_]](gToO: G ~> O) = {
      new (Co[G, F]#Product ~> O) {
        def apply[A](c: Co[G, F]#Product[A]) = c.value match {
          case Left(g) => gToO(g)
          case Right(f) => fToO(f)
        }
      }
    }

    def andThen[G[_]](oToG: O ~> G) =
      new (F ~> G) {
        def apply[A](fa: F[A]) = oToG(fToO(fa))
      }
  }
}