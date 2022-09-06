package dev.akif.tweettracker

import dev.akif.tweettracker.models.TwitterError
import dev.akif.tweettracker.twitter.Twitter
import scala.concurrent.duration.FiniteDuration
import sttp.capabilities.zio.ZioStreams
import sttp.client3.{SttpBackend, SttpBackendOptions}
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.*
import zio.json.*
import zio.logging.backend.SLF4J

import java.io.IOException
import java.util.concurrent.TimeUnit

object Main extends ZIOAppDefault:
  override val bootstrap: ZLayer[Any, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  val asyncHttpClientZioBackend: URLayer[Config, SttpBackend[Task, ZioStreams]] =
    for
      timeout <- ZLayer.fromZIO(ZIO.service[Config].map(_.forSeconds))
      sttp <- AsyncHttpClientZioBackend
        .layer(options = SttpBackendOptions.Default.connectionTimeout(FiniteDuration(timeout.get, TimeUnit.SECONDS)))
        .orDie
    yield sttp

  val config: ULayer[Config] =
    Config.live.orDie

  val twitter: URLayer[SttpBackend[Task, ZioStreams] & Config, Twitter] =
    Twitter.live

  override val run: UIO[ExitCode] =
    Twitter.streamTweets
      .foldZIO(error => ZIO.logError(error.log), tweets => ZIO.logInfo(tweets.toJson))
      .provide(asyncHttpClientZioBackend, config, twitter)
      .exitCode
