package processes.monadTransformers.vanillaScala

import processes.PatchAssignmentSpec

class EnhancedSpec extends PatchAssignmentSpec(
    "Vanilla Scala enhanced monad transformers",
    s => new Enhanced(s)
)