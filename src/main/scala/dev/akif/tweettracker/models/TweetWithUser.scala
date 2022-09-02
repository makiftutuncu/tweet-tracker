package dev.akif.tweettracker.models

import zio.json.*

import java.nio.charset.StandardCharsets

final case class TweetWithUser(id: String, user: User, text: String, createdAt: Long):
  def toTweet: Tweet = Tweet(id, text, createdAt)

object TweetWithUser:
  given tweetWithUserEncoder: JsonEncoder[TweetWithUser] = DeriveJsonEncoder.gen[TweetWithUser]
