package dev.akif.tweettracker

import dev.akif.tweettracker.Twitter.TwitterError
import sttp.capabilities.zio.ZioStreams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.*

trait Twitter:
  def streamTweets(containing: String, forDuration: Duration, upToTweets: Int): IO[TwitterError, Chunk[Tweet]]

object Twitter:
  lazy val live: ZLayer[SttpBackend[Task, ZioStreams] & Config, Nothing, Twitter] =
    ZLayer.fromFunction(TwitterLive.apply)

  enum TwitterError(val message: String):
    case Unknown(override val message: String) extends TwitterError(message)

  def streamTweets(containing: String, forDuration: Duration, upToTweets: Int): ZIO[Twitter, TwitterError, Chunk[Tweet]] =
    ZIO.serviceWithZIO[Twitter](_.streamTweets(containing, forDuration, upToTweets))
