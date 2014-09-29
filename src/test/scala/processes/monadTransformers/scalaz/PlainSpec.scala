package processes.monadTransformers.scalaz

import processes.PatchAssignmentSpec

class PlainSpec extends PatchAssignmentSpec(
    "Scalaz plain monad transformers",
    s => new Plain(s)
)