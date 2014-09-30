package processes.freeMonads.vanillaScala

import processes.PatchAssignmentSpec

class NestedSpec extends PatchAssignmentSpec(
  "Vanilla Scala nested free monads",
  s => new Nested(s)
)