package processes.freeMonads.vanillaScala.multiple

import processes.PatchAssignmentSpec

class HappyFlowSpec extends PatchAssignmentSpec(
  "Vanilla Scala happy flow only (mutiple program types) free monads",
  s => new HappyFlowOnly(s)
)