package processes.freeMonads.vanillaScala.single

import processes.PatchAssignmentSpec

class HappyFlowSpec extends PatchAssignmentSpec(
  "Vanilla Scala happy flow only (single program type) free monads",
  s => new HappyFlowOnly(s)
)