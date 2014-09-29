package processes.monadTransformers.scalaz

import processes.PatchAssignmentSpec

class EnhancedSpec extends PatchAssignmentSpec(
    "Scalaz enhanced monad transformers",
    s => new Enhanced(s)
)