Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / version      := "0.1.0"
ThisBuild / scalaVersion := "3.2.0"

val logbackVersion    = "1.4.0"
val sttpVersion       = "3.7.6"
val zioVersion        = "2.0.2"
val zioConfigVersion  = "3.0.2"
val zioJsonVersion    = "0.3.0-RC10"
val zioLoggingVersion = "2.1.0"

val logback                = "ch.qos.logback"                 % "logback-classic"               % logbackVersion
val sttpAsyncHttpClientZio = "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion
val sttp                   = "com.softwaremill.sttp.client3" %% "core"                          % sttpVersion
val sttpZioJson            = "com.softwaremill.sttp.client3" %% "zio-json"                      % sttpVersion
val zio                    = "dev.zio"                       %% "zio"                           % zioVersion
val zioConfig              = "dev.zio"                       %% "zio-config"                    % zioConfigVersion
val zioConfigTypesafe      = "dev.zio"                       %% "zio-config-typesafe"           % zioConfigVersion
val zioJson                = "dev.zio"                       %% "zio-json"                      % zioJsonVersion
val zioLogging             = "dev.zio"                       %% "zio-logging-slf4j"             % zioLoggingVersion
val zioStreams             = "dev.zio"                       %% "zio-streams"                   % zioVersion
val zioTest                = "dev.zio"                       %% "zio-test"                      % zioVersion % Test
val zioTestSbt             = "dev.zio"                       %% "zio-test-sbt"                  % zioVersion % Test

lazy val root = (project in file("."))
  .settings(
    name := "tweet-tracker",
    libraryDependencies ++= Seq(
      logback,
      sttpAsyncHttpClientZio,
      sttp,
      sttpZioJson,
      zio,
      zioConfig,
      zioConfigTypesafe,
      zioLogging,
      zioStreams,
      zioTest,
      zioTestSbt
    ),
    scalacOptions += "-explain",
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
  .enablePlugins(JavaAppPackaging, DockerPlugin)

import com.typesafe.sbt.packager.docker._

dockerBaseImage := "amazoncorretto:18.0.2-al2"
// By default there are many commands to create a user, group, set permissions etc. which I don't need
dockerCommands := dockerCommands.value.flatMap {
  case Cmd("WORKDIR", _*)                          => Seq(Cmd("WORKDIR", "."))
  case Cmd("COPY", arg) if arg.contains("--chown") => Seq(Cmd("COPY", arg.split(" ").filterNot(_.startsWith("--chown")).mkString(" ")))
  case Cmd("USER", _*)                             => Seq.empty
  case Cmd("RUN", _*)                              => Seq.empty
  case ExecCmd("RUN", _*)                          => Seq.empty
  case c                                           => Seq(c)
}
dockerEnvVars := {
  val keys = Set("SEARCH_TERM", "FOR_SECONDS", "UP_TO_TWEETS", "TOKEN")
  sys.env.filterKeys(keys.contains)
}
dockerLabels       := Map("maintainer" -> "Mehmet Akif Tutuncu <m.akif.tutuncu@gmail.com>")
dockerUpdateLatest := true
