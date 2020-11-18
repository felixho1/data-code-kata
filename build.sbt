name := "data-code-kata"

scalaVersion in ThisBuild := "2.12.8"

val java_opts = List(
  "-XshowSettings:vm",
  "-XX:ReservedCodeCacheSize=1g",
  "-XX:+UseCodeCacheFlushing",
  "-XX:+HeapDumpOnOutOfMemoryError",
  "-XX:MaxRAMFraction=1",
  "-XX:+UnlockExperimentalVMOptions",
  "-XX:+UseCGroupMemoryLimitForHeap"
).mkString(" ")

version := "1.0"

val scalaTest = "3.1.0"
val circeVersion = "0.13.0"
val fs2Version = "2.4.4"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-refined" % circeVersion,
  "co.fs2" %% "fs2-core" % fs2Version,
  "co.fs2" %% "fs2-io" % fs2Version,
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0",
  "joda-time" % "joda-time" % "2.3",
  "org.joda" % "joda-convert" % "1.4",
  "com.typesafe" % "config" % "1.2.1",
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "ch.qos.logback"    %  "logback-classic" % "1.3.0-alpha4",
  "org.slf4j" % "slf4j-log4j12" % "1.7.12" % Test,
  "org.slf4j" % "slf4j-simple" % "1.7.22",
  "org.scalatest" %% "scalatest" % scalaTest % Test
)

test in assembly := {}

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x                             => MergeStrategy.first
}

lazy val root = (project in file("."))
  .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
  .settings(
    version := "1.2.0",
    organization := "ho.felix",
    scalaVersion := scalaVersion.value,
    parallelExecution in Test := false,
    credentials += Credentials(Path.userHome / ".sbt" / ".credentials"),
    javaOptions in Universal ++= Seq("-Duser.timezone=Australia/Sydney"),
    resolvers ++= Seq(
      Resolver.sonatypeRepo("public"),
      Resolver.sonatypeRepo("releases"),
      Resolver.bintrayRepo("spark-packages", "maven")
    ),
    name := "data-code-kata",
    scalacOptions ++= Seq(
      "-Ypartial-unification",
      "-unchecked",
      "-feature",
      "-deprecation",
      "-encoding",
      "UTF-8",
      "-Xfatal-warnings",
      "-language:higherKinds"),
    imageNames in docker := Seq(
      ImageName(s"${name.value}:latest")
    ),
    dockerfile in docker := {
      val appDir: File = stage.value
      val targetDir = "/app"
      new Dockerfile {
        from("openjdk:8-jdk")
        env(
          "JAVA_OPTS" -> java_opts,
          "PATH" -> "/app/bin:${PATH}"
        )
        copy(appDir, targetDir)
        workDir("/app/bin")
      }
    }
  )

