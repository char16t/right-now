ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.2.1"

lazy val root = (project in file("."))
  .settings(
    name := "neo4j",
    libraryDependencies ++= Seq(
      "org.neo4j.driver" % "neo4j-java-driver" % "5.3.1",
    )
  )
