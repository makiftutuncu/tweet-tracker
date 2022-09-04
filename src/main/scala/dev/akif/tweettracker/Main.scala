package dev.akif.tweettracker

import dev.akif.tweettracker.twitter.Twitter
import dev.akif.tweettracker.twitter.Twitter.TwitterError
import sttp.capabilities.zio.ZioStreams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.*
import zio.json.*
import zio.logging.backend.SLF4J

import java.io.IOException

object Main extends ZIOAppDefault:
  val asyncHttpClientZioBackend: ULayer[SttpBackend[Task, ZioStreams]] =
    AsyncHttpClientZioBackend.layer().orDie

  val config: ULayer[Config] =
    Config.live

  val twitter: URLayer[SttpBackend[Task, ZioStreams] & Config, Twitter] =
    Twitter.live

  val logger: ZLogger[String, Option[Unit]] =
    SLF4J
      .slf4jLogger(SLF4J.logFormatDefault, SLF4J.getLoggerName())
      .filterLogLevel(_ >= LogLevel.Trace)

  override val run: UIO[ExitCode] =
    Twitter.streamTweets
      .foldZIO(error => ZIO.fail(RuntimeException(error.log)), tweets => ZIO.logInfo(tweets.toJson))
      .provide(asyncHttpClientZioBackend, config, twitter)
      .provideLayer(Runtime.removeDefaultLoggers >>> Runtime.addLogger(logger))
      .exitCode
