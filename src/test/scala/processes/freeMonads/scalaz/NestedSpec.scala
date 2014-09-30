package processes.freeMonads.scalaz

import processes.PatchAssignmentSpec

class NestedSpec extends PatchAssignmentSpec(
  "Scalaz nested free monads",
  s => new Nested(s)
)