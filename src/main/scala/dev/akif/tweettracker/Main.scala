package dev.akif.tweettracker

import sttp.capabilities.zio.ZioStreams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.*
import zio.logging.backend.SLF4J

import java.io.IOException

object Main extends ZIOAppDefault:
  private lazy val asyncHttpClientZioBackend = AsyncHttpClientZioBackend.layer().orDie
  private lazy val config                    = Config.live
  private lazy val twitter                   = Twitter.live
  private lazy val logger                    = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override val run: UIO[ExitCode] =
    Twitter
      .streamTweets(containing = "crypto", forDuration = 30.seconds, upToTweets = 100)
      .foldCauseZIO(cause => ZIO.logErrorCause("Failed to stream tweets", cause), tweets => ZIO.logInfo(tweets.toString))
      .provide(asyncHttpClientZioBackend, config, twitter)
      .provideLayer(logger)
      .exitCode
