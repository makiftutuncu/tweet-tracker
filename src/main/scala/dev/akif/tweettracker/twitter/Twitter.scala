package dev.akif.tweettracker.twitter

import dev.akif.tweettracker.models.{TweetWithUser, Tweets}
import dev.akif.tweettracker.twitter.Twitter.TwitterError
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

  enum TwitterError(val message: String, val details: String):
    case RequestFailed(override val details: String) extends TwitterError("Request to stream tweets failed", details)

    case CannotParseTweet(why: String, json: String) extends TwitterError(s"Cannot parse tweet", s"$why, payload was: $json")

    val log: String = s"$message: $details"

  def streamTweets: ZIO[Twitter, TwitterError, Tweets] =
    ZIO.serviceWithZIO[Twitter](_.streamTweets)
