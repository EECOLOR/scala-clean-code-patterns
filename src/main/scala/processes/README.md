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

- `domain` - Contains the profile representation
- `services` - Contains the different methods that are at our disposal
- `results` - Contains the different results that are expected

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

val program:Free[ReturnType, String] =
  for {
    a <- MyMethod("test", 8)
    b <- MyMethod("green", 4)
  } yield {
    if (a && b) "yes" else "no"
  }
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

The first example is the [`HappyFlowOnly`](freeMonads/vanillaScala/HappyFlowOnly.scala) class.
This version focuses on the happy flow of the program and moves handling of the 
not found, bad request, etc. results to the program runner.

The second example is the [`Complete`](freeMonads/vanillaScala/Complete.scala) class.
This version enhances the `Method` instances with functions like `ifEmpty` and 
`ifError` that wrap the method call into another method call. This moves the non 
happy cases into the the `handlePatchRequest` method.