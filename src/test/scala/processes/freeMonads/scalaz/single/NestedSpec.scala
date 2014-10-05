package processes.freeMonads.scalaz.single

import processes.PatchAssignmentSpec

class NestedSpec extends PatchAssignmentSpec(
  "Scalaz nested free monads",
  s => new Nested(s)
)