package processes.freeMonads.scalaz

import processes.PatchAssignmentSpec
import processes.freeMonads.scalaz.single.Complete

class CompleteSpec extends PatchAssignmentSpec(
    "Scalaz complete free monads",
    s => new Complete(s)
)