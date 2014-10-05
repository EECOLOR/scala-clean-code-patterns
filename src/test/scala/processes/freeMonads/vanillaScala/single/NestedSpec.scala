package processes.freeMonads.vanillaScala.single

import processes.PatchAssignmentSpec

class NestedSpec extends PatchAssignmentSpec(
  "Vanilla Scala nested free monads",
  s => new Nested(s)
)