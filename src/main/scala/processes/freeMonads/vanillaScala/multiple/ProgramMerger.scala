package processes.freeMonads.vanillaScala.multiple

import processes.freeMonads.vanillaScala.MultipleMachinery
import processes.freeMonads.multiple.CompleteProgramParts
import scala.language.higherKinds
import scala.concurrent.Future

/*
 * Warning, this file contains stuff that might melt your brain. Type level 
 * programming relies (just as some functional programming) a lot on recursive 
 * style thingies. Human brains seem not to be able to handle that very good. 
 */
trait ProgramMerger { _: MultipleMachinery with CompleteProgramParts =>

  implicit class ProgramEnhancement[O[_], A](program: Free[O, A]) {
    def mergeBranch(implicit merger: BranchMerger[O, A]) = merger merged program
  }

  trait BranchMerger[O[_], A] {
    type Out[_]
    def merged(program: Free[O, A]): Free[Out, A]
  }

  object BranchMerger {

    implicit def merger[O[_], T](
      implicit containsMergableBranch: Branch[T]#Instance ~> O,
      result: O - Branch[T]#Instance) = {

      // This is an intermediary type to store the branch. T and A will be 
      // the same (containsMergeableBranch, see above) proves that.  
      type FreeWithoutBranch[A] = Free[result.Out, Either[T, A]]

      // To turn Free[O, A] into FreeWithoutBranch[A] we need FreeWithoutBranch
      // to have a monad. 
      implicit val freeWithoutBranchMonad = new Monad[FreeWithoutBranch] {
        def create[A](a: A): Free[result.Out, Either[T, A]] = Free(Right(a))
        def flatMap[A, B](fa: Free[result.Out, Either[T, A]])(f: A => Free[result.Out, Either[T, B]]) =
          fa.flatMap {
            case Left(t) => Free[result.Out, Either[T, B]](Left(t))
            case Right(a) => f(a)
          }
      }

      // A way to extract the value of the branch and put it into the new
      // free instance
      val mapper = new (O ~> FreeWithoutBranch) {
        def apply[A](o: O[A]): FreeWithoutBranch[A] =
          result.removeFrom(o) match {
            case Left(a) => Free(Left(a.value))
            case Right(b) => Free.lift(b).map(Right.apply)
          }
      }

      // merging now seems simple
      new BranchMerger[O, T] {
        type Out[x] = result.Out[x]
        def merged(program: Free[O, T]): Free[Out, T] =
          program.run(mapper).map(_.merge)
      }
    }
  }

  // Type to represent a removal. We remove T from O
  trait Remove[O[_], T[_]] {
    type Out[_]

    // If T was in there, we remove it, otherwise we return the result
    def removeFrom[A](o: O[A]): Either[T[A], Out[A]]
  }

  // Simple alias to allow the fance O - T notation
  type -[O[_], T[_]] = Remove[O, T]

  object Remove {

    implicit def atHead[T[_], Tail[_]] =
      new Remove[Co[T, Tail]#Product, T] {
        type Out[x] = Tail[x]
        def removeFrom[A](o: Co[T, Tail]#Product[A]): Either[T[A], Out[A]] =
          o.value
      }

    implicit def atTail[T[_], Head[_]] =
      new Remove[Co[Head, T]#Product, T] {
        type Out[x] = Head[x]
        def removeFrom[A](o: Co[Head, T]#Product[A]): Either[T[A], Out[A]] =
          o.value.swap
      }

    implicit def inTail[T[_], Head[_], Tail[_]](
      implicit resultType: Tail - T) =
      new Remove[Co[Head, Tail]#Product, T] {
      
        type Out[x] = Co[Head, resultType.Out]#Product[x]
        
        def removeFrom[A](o: Co[Head, Tail]#Product[A]): Either[T[A], Out[A]] =
          o.value match {
            case Left(a) => 
              Right(Co[Head, resultType.Out].Product(Left(a)))
            case Right(b) =>
              resultType.removeFrom(b) match {
                case Left(a) => Left(a)
                case Right(b) => Right(Co[Head, resultType.Out].Product(Right(b)))
              }
          }
      }
  }
}