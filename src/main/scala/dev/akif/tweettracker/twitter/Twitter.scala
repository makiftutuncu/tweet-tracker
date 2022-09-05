package dev.akif.tweettracker.twitter

import dev.akif.tweettracker.models.{TweetWithUser, Tweets, TwitterError}
import dev.akif.tweettracker.twitter.TwitterLive
import dev.akif.tweettracker.Config
import sttp.capabilities.zio.ZioStreams
import sttp.client3.SttpBackend
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio.*

trait Twitter:
  def streamTweets: IO[TwitterError, Tweets]

object Twitter:
  val live: ZLayer[SttpBackend[Task, ZioStreams] & Config, Nothing, Twitter] =
    ZLayer.fromFunction(TwitterLive.apply)

  def streamTweets: ZIO[Twitter, TwitterError, Tweets] =
    ZIO.serviceWithZIO[Twitter](_.streamTweets)
