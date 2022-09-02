package dev.akif.tweettracker.twitter.models

import dev.akif.tweettracker.models.{TweetWithUser, User}
import zio.json.*

final case class TwitterResponse(data: TwitterData, includes: TwitterIncludes):
  def toTweetWithUser: TweetWithUser =
    TweetWithUser(
      id = data.id,
      user = includes.users
        .find(u => u.id == data.author_id)
        .map { u =>
          User(id = u.id, name = u.name, username = u.username, createdAt = u.created_at.toEpochMilli)
        }
        .getOrElse(throw Exception(s"Cannot find user ${data.author_id} in users, incorrect data from Twitter?")),
      text = data.text,
      createdAt = data.created_at.toEpochMilli
    )

object TwitterResponse:
  given twitterResponseDecoder: JsonDecoder[TwitterResponse] = DeriveJsonDecoder.gen[TwitterResponse]
