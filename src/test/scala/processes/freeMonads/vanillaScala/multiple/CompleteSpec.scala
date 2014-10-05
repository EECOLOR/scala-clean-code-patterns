package processes.freeMonads.vanillaScala.multiple

import processes.PatchAssignmentSpec

class CompleteSpec extends PatchAssignmentSpec(
  "Vanilla Scala complete (mutiple program types) free monads",
  s => new Complete(s)
)