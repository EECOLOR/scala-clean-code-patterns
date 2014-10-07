package processes.freeMonads.vanillaScala.multiple

import processes.freeMonads.multiple.CompleteProgramParts
import processes.freeMonads.vanillaScala.MultipleMachinery
import scala.language.higherKinds

/*
 * Defined in a separate file because otherwise the lookup of the implicits failed.
 */
trait BranchingUtils { _: MultipleMachinery with CompleteProgramParts =>

  trait Brancher[F[_], G[_], L] {
    type Out[x]
    def branch[R, A](right: F[A])(branch: A => Either[G[L], R]): Free[Out, R]
  }

  object Brancher {

    /*
     * This looks like a scary method, and it probably is. It uses the A value in F[A]
     * to determine if it needs to branch. If it needs to branch it will secretly
     * (using the Branch[L]#Instance[R] type) insert the branch based on the L value 
     * in G[L].
     * 
     * It becomes a bit complicated because we do not know anything about F[_] and 
     * G[_], to get at the values that are inside of them we need to wrap them in 
     * Free. In order to wrap them in Free we need to know a few things:
     * - The program type O[_]
     * - A way to inject F into O
     * - A way to inject G into O
     * 
     * Last but not least we need to inject the branch into O
     */
    implicit def forProgram[F[_], G[_], L, O[_]](
      implicit programType: ProgramType[O],
      injectF: F ~> O,
      injectG: G ~> O,
      injectBranch: Branch[L]#Instance ~> O) =
      new Brancher[F, G, L] {
        type Out[x] = O[x]

        def branch[R, A](right: F[A])(branch: A => Either[G[L], R]): Free[O, R] =
          right.flatMap { a =>
            branch(a) match {
              case Left(left) => left flatMap Branch[L, R]
              case Right(r) => Free(r)
            }
          }
      }
  }
}