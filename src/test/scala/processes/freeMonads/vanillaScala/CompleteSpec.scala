package processes.freeMonads.vanillaScala

import processes.PatchAssignmentSpec

class CompleteSpec extends PatchAssignmentSpec(
    "Vanilla Scala complete free monads",
    s => new Complete(s)
)