package processes.vanillaScala

import processes.PatchAssignmentSpec

class VanillaScalaSpec extends PatchAssignmentSpec(
    "Vanilla Scala",
    s => new VanillaScala(s)
)