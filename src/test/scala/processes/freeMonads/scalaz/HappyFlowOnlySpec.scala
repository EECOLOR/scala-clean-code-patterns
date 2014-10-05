package processes.freeMonads.scalaz

import processes.PatchAssignmentSpec
import processes.freeMonads.scalaz.single.HappyFlowOnly

class HappyFlowSpec extends PatchAssignmentSpec(
    "Scalaz happy flow only free monads",
    s => new HappyFlowOnly(s)
)
