name := "AmdhalDemonstration"
     
version := "0.1"
     
scalaVersion := "2.11.8"

fork := true

javaOptions in run += "-Xmx8G"

libraryDependencies +=
  "com.typesafe.akka" %% "akka-actor" % "2.4.9-RC2"

lazy val commonSettings = Seq(
  version := "0.1",
  organization := "org.amdahl",
  scalaVersion := "2.11.8",
  test in assembly := {}
)
