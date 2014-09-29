name := "scala-clean-code-patterns"

organization := "org.qirx"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.1.0",
  "com.typesafe.play" %% "play" % "2.3.4",
  "com.typesafe.play" %% "play-json" % "2.3.4",
  "com.typesafe.play" %% "play-test" % "2.3.4" % "test",
  "org.qirx" %% "little-spec" % "0.3" % "test"
)

testFrameworks += new TestFramework("org.qirx.littlespec.sbt.TestFramework")
