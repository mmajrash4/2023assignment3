lazy val root = (project in file(".")).
  settings(
    name := "future promise actor",
    version := "1.0",
    scalaVersion := "2.12.6"
  )

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.3",
  "com.typesafe.play" %% "play-ahc-ws-standalone" % "2.0.0-M1",
  "com.typesafe.play" %% "play-json" % "2.6.9",
  "org.scalactic" %% "scalactic" % "3.0.5",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
