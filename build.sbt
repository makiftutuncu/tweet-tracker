Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.2.0"

lazy val logbackVersion    = "1.4.0"
lazy val sttpVersion       = "3.7.6"
lazy val zioVersion        = "2.0.2"
lazy val zioLoggingVersion = "2.1.0"

lazy val logback                = "ch.qos.logback"                 % "logback-classic"               % logbackVersion
lazy val sttpAsyncHttpClientZio = "com.softwaremill.sttp.client3" %% "async-http-client-backend-zio" % sttpVersion
lazy val sttp                   = "com.softwaremill.sttp.client3" %% "core"                          % sttpVersion
lazy val sttpZioJson            = "com.softwaremill.sttp.client3" %% "zio-json"                      % sttpVersion
lazy val zio                    = "dev.zio"                       %% "zio"                           % zioVersion
lazy val zioLogging             = "dev.zio"                       %% "zio-logging-slf4j"             % zioLoggingVersion
lazy val zioStreams             = "dev.zio"                       %% "zio-streams"                   % zioVersion
lazy val zioTest                = "dev.zio"                       %% "zio-test"                      % zioVersion % Test

lazy val root = (project in file("."))
  .settings(
    name := "tweet-tracker",
    libraryDependencies ++= Seq(logback, sttpAsyncHttpClientZio, sttp, sttpZioJson, zio, zioLogging, zioStreams, zioTest),
    scalacOptions += "-explain"
  )
