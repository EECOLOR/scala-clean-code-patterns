package processes.freeMonads.vanillaScala

import processes.PatchAssignmentSpec

class HappyFlowSpec extends PatchAssignmentSpec(
  "Vanilla Scala happy flow only free monads",
  s => new HappyFlowOnly(s)
)