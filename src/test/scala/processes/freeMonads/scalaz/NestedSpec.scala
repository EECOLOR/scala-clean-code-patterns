package processes.freeMonads.scalaz

import processes.PatchAssignmentSpec
import processes.freeMonads.scalaz.single.Nested

class NestedSpec extends PatchAssignmentSpec(
  "Scalaz nested free monads",
  s => new Nested(s)
)