package dev.akif.tweettracker

import dev.akif.tweettracker.Twitter.TwitterError

import scala.concurrent.duration.Duration as ScalaDuration
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.Uri
import zio.*
import zio.stream.{ZSink, ZStream}

import java.util.concurrent.TimeUnit

final case class TwitterLive(sttp: SttpBackend[Task, ZioStreams], config: Config) extends Twitter:
  private lazy val getStream: IO[TwitterError, ZioStreams.BinaryStream] =
    sttp
      .send(
        basicRequest
          .get(TwitterLive.streamUri)
          .auth
          .bearer(config.token)
          .response(asStreamUnsafe(ZioStreams))
      )
      .flatMapError { error =>
        val message = "Request to stream tweets failed"
        ZIO.logErrorCause(message, Cause.fail(error)).as(TwitterError.Unknown(message))
      }
      .flatMap { response =>
        response.body match
          case Left(error) =>
            val message = "Request to stream tweets failed"
            ZIO.logErrorCause(message, Cause.fail(error)) *> ZIO.fail(TwitterError.Unknown(message))

          case Right(stream) =>
            ZIO.succeed(stream)
      }

  override def streamTweets(containing: String, forDuration: Duration, upToTweets: Int): IO[TwitterError, Chunk[Tweet]] =
    for
      _ <- ZIO.logInfo(s"Streaming tweets containing '$containing' for ${forDuration.toSeconds} seconds or up to $upToTweets tweets")
      _ <- getStream
    yield Chunk.empty

object TwitterLive:
  val streamUri: Uri = uri"https://api.twitter.com/2/tweets/search/stream"
