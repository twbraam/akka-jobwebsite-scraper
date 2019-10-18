name := "akka-ping-pong"

version := "0.1"

scalaVersion := "2.13.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka"        %% "akka-actor-typed"       % "2.5.26",
  "com.typesafe.akka"        %% "akka-persistence-typed" % "2.5.26",
  "org.scalatest"            %% "scalatest"              % "3.0.8" % Test
)
