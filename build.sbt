lazy val root = (project in file(".")).
  settings(
    name := "cosc250assignment3",
    version := "2021.0",
    scalaVersion := "3.0.0-RC3"
  )

libraryDependencies ++= Seq(
  ("com.typesafe.akka" % "akka-actor" % "2.6.14").cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" % "akka-actor-typed" % "2.6.14").cross(CrossVersion.for3Use2_13),
  ("com.typesafe.akka" % "akka-stream" % "2.6.14").cross(CrossVersion.for3Use2_13),
)

libraryDependencies += "org.scalameta" %% "munit" % "0.7.25" % Test

// We also need to register munit as a test framework in sbt so that "sbt test" will work and the IDE will recognise
// tests
testFrameworks += new TestFramework("munit.Framework")
