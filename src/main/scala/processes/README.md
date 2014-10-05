# Processes

There are different approaches to writing down processes in Scala code. This part 
contains explorations using different designs and different techniques.

When creating software there are different aspects that needs to be taken into 
account when choosing a design or technique. There are roughly 3 ways to encode 
processes:

1. [Vanilla Scala](#vanilla-scala) - Structure, execution and implementation are tightly coupled
2. [Monad transformers](#monad-transformers) - Structure and implementation are coupled, but execution is decoupled
3. [Free monads](#free-monads) - Structure, implementation and execution are decoupled

## Patch assignment

The assignment is to implement the handler for patching a profile. We are using 
the Play Framework as an example. In real applications you can image the following 
route:

```
PATCH  /api/profile/:id  app.http.ProfileConstroller.patch(id)
```

The [`PatchAssignment`](PatchAssignment.scala) class contains the method that the 
different implementations should provide. It has the following signature:

```scala
protected def handlePatchRequest(id: String, request: Request[AnyContent]): Future[Result]
```

It also contains serveral utilities:

- `results` - Contains the different results that are expected
- `defaultExecutionContext` - an implicit execution context

The [`Services`](Services.scala) class contains the available service methods each 
implementation can use.

### Vanilla Scala

The [VanillaScala](vanillaScala/VanillaScala.scala) class contains a straightforward 
implementation without any bells and whistles. It small methods with descriptive 
names and uses methods from the standard library.

*Pros*

- No framework knowledge (outside of the standard library) is required
- A small amount of code is involved
- No knowledge of concepts like `Monad` is required
- Very little boilerplate

*Cons*
- It's a puzzle to figure out what is going on. The 'important' parts of the code 
  are distributed through chained functions
- Not easy to unit test parts of the process
- Structure, implementation and execution are tightly coupled (can be a pro as well)


### Monad transformers

This method makes use of the sugar syntax for `map` and `flatMap` called 'for 
comprehension'.

```scala
xa.flatMap { a =>
  yb.map { => b
    a + b
  }
}
```
Can also be written as
```scala
for {
  a <- xa
  b <- yb
} yield a + b
```

This allows us to write nested code in a (visually) non nested fashion. Without 
knowing exactly what's going on our brains don't have a hard time to guess what 
happens.

An important restriction of this syntax is that all stuff on the right side of the 
`<-` must be of the same (container) type. In the previous example `xa` and `yb` 
must be of the same (container) type.

In real applications this is however hardly ever the case. Some methods return 
`Option[_]` while others return a `Future[_]` and even others return 
`Future[Option[_]]`. That's where monad transformers come in. They are containers 
for (container) types, allowing you to `map` and `flatMap` over the 'important' value. 
So for example they allow you to `map` over `A` in a `Future[Option[A]]`.

To learn more about monad transformers check the presentation [Flatten your code](https://speakerdeck.com/eamelink/flatten-your-code)
and [source code / exercises](https://github.com/eamelink/flatten) by Erik Bakker.

There are two implementations of this pattern, one using [scalaz](https://github.com/scalaz/scalaz)
and one using the same classes implemented in vanilla Scala. For each of these 
implementations we provide a `Plain` and an `Enhanced` version. The `Enhanced` 
version adds some extra sugar to improve the readability of the code.

The vanilla Scala version has been added to show what is actually happening without 
being intimidated by the source code of the Scalaz library.

The [`HttpResultImplementation`](monadTransformers/HttpResultImplementation.scala) class
is provided with a generic version of methods that can lift values into the monad 
transformer (`EitherT` in this case). The [`ScalazMachinery`](monadTransformers/scalaz/ScalazMachinery.scala) class
and [`Machinery`](monadTransformers/vanillaScala/Machinery.scala) classes provide a concrete 
implementation of `HttpResultImplementation`. 

The [`Enhancements`](monadTransformers/Enhancements.scala) class builds on top of the 
`HttpResultImplementation` and provides a set of implicit classes to polish the lifting
to the transformer. 

The versions:

- [scalaz.Plain](monadTransformers/scalaz/Plain.scala)
- [scalaz.Enhanced](monadTransformers/scalaz/Enhanced.scala)
- [vanillaScala.Plain](monadTransformers/vanillaScala/Plain.scala)
- [vanillaScala.Enhanced](monadTransformers/vanillaScala/Enhanced.scala)

*Pros*

- Clearly conveys the important parts of the process
- Relatively easy to understand
- Separates the construction and implementation from the execution
- Small amount of boilerplate

*Cons*

- You need to know the resulting type when you start, this can be tricky. In this 
  example we needed an `EitherT[Future, Result, A]`
- Every step in the for comprehension needs to be lifted to the same type. It
  is required that the result of the method is lifted in place. This means that 
  you are using different kinds of lifts in every for comprehension
- Requires understanding of monad transformers

### Free monads

This method makes (just as the monad transformers) use of the for comprehension 
sugar syntax.

The concept here is that we decouple the a function and it's body.

```scala
def myMethod(a:String, b:Int):Boolean = ???
```

The `myMethod` function also be written like this:

```scala
sealed trait ReturnType[T]
case class MyMethod(a:String, b:Int) extends ReturnType[Boolean]
```

The above structure allows us to store the arguments of the function and encode the 
return type without having to specify the function body (`???` in `myMethod`).

The `Free` class capitalize on this concept by providing `map` and `flatMap` functions
for classes that have the following structure: `F[_]`. Note that `ReturnType[T]` in 
our example has this exact structure. 

If we provide an implicit conversion from a `F[_]` style class to the `Free` class 
we get `map` and `flatMap` for free!

```scala
implicit def toFree[F[_], A](fa: F[A]): Free[F, A] = ???

val program =
  for {
    a <- MyMethod("test", 8)
    b <- MyMethod("green", 4)
  } yield {
    if (a && b) "yes" else "no"
  }

// The type of the program is Free[ReturnType, String]  
```

You might have noticed that `Free` captures both the `F[_]` type (here `ReturnType`) and 
the result of the program (`String` in this case).

`Free` allows us to construct programs purely on wishful thinking. As you might have 
noticed we did not write any implementation yet. The implementation only becomes 
important when we want to run the program.

As you might have guessed `Free` is actually cheating. It pretends to execute `map` 
and `flatMap` on arbitrary types. It is however impossible to retrieve the result of 
the program until you provide a 'runner'. This runner converts the `F[_]` to another 
container type `G[_]` and guess what, it requires you to provide a monad for `G[_]`.

So, to run a program, you need:
- A transformer from `F[_]` to `G[_]` (also called natural transformation)
- A monad for `G[_]`

An example to explain how we would run the above program.

```scala
// our `G[_]` type
type Id[A] = A

object ProgramRunner extends (ReturnType ~> Id) {
  def apply[A](ra:ReturnType[A]):Id[A] = ra match {
    case MyMethod(a, b) =>
      // the body, should return a Boolean otherwise you will get a compile error
      ???
  }
}

implicit val idMonad = new Monad[Id] {
  def create[A](value:A):Id[A] = value
  def flatMap[A, B](fa:Id[A])(f: A => Id[B]):Id[B] = f(fa) 
}

// Note that Id[String] and String are the same
val result:Id[String] = program.run(ProgramRunner)
```

The above example is of course too simple. In reality we have much more complicated 
types like `Future`, `Either` and `Option`. The examples provide realistic versions 
of the programs.

The first example is the [`HappyFlowOnly`](freeMonads/vanillaScala/single/HappyFlowOnly.scala) class.
This version focuses on the happy flow of the program and moves handling of the 
not found, bad request, etc. results to the program runner.

The second example is the [`Complete`](freeMonads/vanillaScala/single/Complete.scala) class.
This version enhances the `Method` instances with functions like `ifEmpty` and 
`ifError` that wrap the method call into another method call. This moves the non 
happy cases into the the `handlePatchRequest` method.

The third version is the [`Nested`](freeMonads.vanillaScala/single/Nested.scala) class.
This version uses a nested structure. You can imagine this version being a one on 
one representation from a white board session. The main process consists of three 
sub routines which themselves can also be a process of multiple steps. There are 
many different ways of encoding this pattern, I choose to put the program of the 
sub routine in the subroutine itself, but it could have been put in the runner as 
well.  

All versions are implemented using Scalaz as well:

- [`scalaz.single.HappyFlowOnly`](freeMonads/scalaz/single/HappyFlowOnly.scala)
- [`scalaz.single.Complete`](freeMonads/scalaz/single/Complete.scala)
- [`scalaz.single.Nested`](freeMonads/scalaz/single/Nested.scala)

It is possible have multiple types of programs. In the above examples a program part 
extends `Method[ReturnType]`, it is however possible to combine multiple types. This 
is done using a type called `Coproduct`. `Coproduct` is similar to `Either`, but it 
operates on container types. We could have a `Coproduct` that represents a `String` 
in `F[_]` or `G[_]`. With `Either` we can only represent `F[String]` or `G[String]` 
loosing the information that `F` and `G` are container types.

Things will become more complex when we introduce these program combinators, but they 
also allow us to compose programs more freely and from different sources.

The talk [Compositional application architecture with reasonably priced monads](https://parleys.com/play/53a7d2c3e4b0543940d9e538) 
with [the slides](https://t.co/QsBDMDqGGE) and [code](https://gist.github.com/runarorama/a8fab38e473fafa0921d) by Rúnar Bjarnason provide a good explanation about this 
topic. 

The basics are the same but because we are using multiple program types, the 
definition of the implicit conversion to `Free` looks different:

```scala
implicit def toFree[F[_], A, O[_]](fa: F[A])(
  implicit programType: ProgramType[O], insert: F ~> O): Free[O, A] =
  
trait ProgramType[F[_]]
```

As you can see we introduced a trait called `ProgramType` that acts as a magnet for 
the resulting program type `O[_]`. Let's say that the program type `O[_]` is 
`Coproduct[Group1, Group2, _]`, then we need to have a way to insert a `GroupX` 
instance at the correct location (left or right). We can use a natural 
transformation to do that `F ~> O`. In our case we would have either
`Group1 ~> Coproduct[Group1, Group2, _]` or `Group2 ~> Coproduct[Group1, Group2, _]`.

Luckily we are able to generalize those natural transformations because they follow 
a pattern. There are only 3 types of transformations:

**`F ~> F`**

When type `F` is equal to `O`.

**`F ~> Coproduct[F, X, _]`**

When `O` is a `Coproduct` and type `F` is at the left side.

**`F ~> Coproduct[X, Y, _]`**

When `O` is a `Coproduct` and there is a natural transformation for `F ~> Y` meaning
it's in somewhere at the right side. `Y` here can be either `F` itself or another 
`Coproduct`.

A program using multiple program types then looks like this:

```scala
type Co[F[_], G[_]] = {
  type Product[A] = Coproduct[F, G, A]
}
/*
  In my opinion a type alias is more clear than an anonymous type (or type lambda), 
  we could have wrote `({ type T[A] = Coproduct[Group1, Group2, A] })#T` instead 
  of `Co[Group1, Group2]#Product`
*/ 
implicit val programType:ProgramType[Co[Group1, Group2]#Product] = null

val program =
  for {
    value1 <- Group1Method()
    value2 <- Group2Method(value1)
  } yield value2
```

Note: In the examples I use another way of creating the program type, for the above 
example it looks like this:

```scala
implicit val programType = ProgramType[Group1 +: Group2 +: Nil]
```

The result is the same, but it uses some type level ninja stuff to keep things 
readable. You can imagine things become unreadable quite fast with multiple nested 
coproducts.

Running a combined program gives us two challenges. The first is that we have runners
for the different parts of the program that need to be combined. The second is that 
these runners not necessarily result in the same type.

To overcome these problems we need an `or` and `andThen` function for our natural 
transformations. 

The `or` function allows us to combine runners (that result in the same type) into a
runner that can handle coproducts. It has the following signature: 

`or[F[_], G[_], H[_](fToH: F ~> H, gToH: G ~> H): Co[F, G]#Product ~> H`

The `andThen` function allows us to chain runners. We can use it to align the types 
(required for the `or` method). It has the following signature:

`andThen[F[_], G[_], H[_]](fToG: F ~> G, gToH: G ~> H):F ~> H`

Runners for the above program could look like this:

```scala
val programRunner = {
  val group1Runner = Group1Runner andThen IdToFuture
  Group2Runner or group1Runner
} 

object Group1Runner extends (Group1 ~> Id) {
  ...
}

object Group2Runner extends (Group2 ~> Future) {
  ...
}

object IdToFuture extends (Id ~> Future) {
  ...
}
```

The first example is the [`HappyFlowOnly`](freeMonads/vanillaScala/multiple/HappyFlowOnly.scala) class.
This version focuses on the happy flow of the program and moves handling of the 
not found, bad request, etc. results to the program runner. It is similar to 'single' 
version and shows the basics of working with a program that consists of multiple 
parts.

*in progress*   