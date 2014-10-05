package processes.freeMonads.scalaz.single

import processes.PatchAssignmentSpec

class HappyFlowSpec extends PatchAssignmentSpec(
    "Scalaz happy flow only free monads",
    s => new HappyFlowOnly(s)
)
