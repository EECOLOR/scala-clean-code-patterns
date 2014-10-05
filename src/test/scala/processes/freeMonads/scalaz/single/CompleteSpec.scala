package processes.freeMonads.scalaz.single

import processes.PatchAssignmentSpec

class CompleteSpec extends PatchAssignmentSpec(
    "Scalaz complete free monads",
    s => new Complete(s)
)