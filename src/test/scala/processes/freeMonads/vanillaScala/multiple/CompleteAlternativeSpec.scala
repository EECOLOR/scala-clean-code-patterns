package processes.freeMonads.vanillaScala.multiple

import processes.PatchAssignmentSpec

class CompleteAlternativeSpec extends PatchAssignmentSpec(
  "Vanilla Scala complete alternative (mutiple program types) free monads",
  s => new CompleteAlternative(s)
)