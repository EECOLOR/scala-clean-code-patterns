package processes.freeMonads.vanillaScala.single

import processes.PatchAssignmentSpec

class CompleteSpec extends PatchAssignmentSpec(
  "Vanilla Scala complete free monads",
  s => new Complete(s)
)