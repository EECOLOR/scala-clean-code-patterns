package processes.monadTransformers.vanillaScala

import processes.PatchAssignmentSpec

class PlainSpec extends PatchAssignmentSpec(
    "Vanilla Scala plain monad transformers",
    s => new Plain(s)
)