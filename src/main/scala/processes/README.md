# Processes

There are different approaches to writing down processes in Scala code. This part 
contains explorations using different designs and different techniques.

When creating software there are different aspects that needs to be taken into 
account when choosing a design or technique. There are roughly 3 ways to encode 
processes:

1. Structure, execution and implementation are tightly coupled
2. Structure and implementation are coupled, but execution is decoupled
3. Structure, implementation and execution is decoupled

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

There are two implementations of this pattern, one using [scalaz](https://github.com/scalaz/scalaz)
and one using the same classes implemented in vanilla Scala. For each of these 
implementations we provide a `Plain` and an `Enhanced` version. The `Enhanced` version 
adds some extra sugar to improve the readability of the code.

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

Pros:

- Clearly conveys the important parts of the process
- Relatively easy to understand
- Separates the construction and implementation from the execution
- Small amount of boilerplate

Cons:

- You need to know the resulting type when you start, this can be tricky. In this 
  example we needed an `EitherT[Future, Result, A]`
- Every step in the for comprehension needs to be lifted to the same type. It
  is required that the result of the method is lifted in place. This means that 
  you are using different kinds of lifts in every for comprehension
- Requires understanding of monad transformers
