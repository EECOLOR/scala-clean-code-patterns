package processes.freeMonads.scalaz.multiple

import processes.PatchAssignmentSpec

class HappyFlowSpec extends PatchAssignmentSpec(
  "Scalaz happy flow only (mutiple program types) free monads",
  s => new HappyFlowOnly(s)
)