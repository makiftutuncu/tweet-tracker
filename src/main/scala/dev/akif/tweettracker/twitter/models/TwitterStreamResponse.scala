package dev.akif.tweettracker.twitter.models

import dev.akif.tweettracker.models.{TweetWithUser, User}
import zio.*
import zio.json.*

final case class TwitterStreamResponse(data: TwitterData, includes: TwitterIncludes, matching_rules: List[TwitterMatchingRule]):
  def toTweetWithUserFor(ruleId: String): UIO[Option[TweetWithUser]] =
    if !matching_rules.exists(_.id == ruleId) then ZIO.logTrace(s"Tweet '${data.id}' does not match to rule id '$ruleId'").as(None)
    else
      val maybeUser = includes.users
        .find(u => u.id == data.author_id)
        .map { u =>
          User(id = u.id, name = u.name, username = u.username, createdAt = u.created_at.toEpochMilli)
        }

      ZIO
        .fromOption(maybeUser)
        .orDieWith(_ => RuntimeException(s"Cannot find user '${data.author_id}' in users, received incorrect data from Twitter"))
        .flatMap { user =>
          val tweet = TweetWithUser(id = data.id, user = user, text = data.text, createdAt = data.created_at.toEpochMilli)
          ZIO.logDebug(s"Got tweet: ${tweet.toJson}").as(Some(tweet))
        }

object TwitterStreamResponse:
  given twitterStreamResponseDecoder: JsonDecoder[TwitterStreamResponse] = DeriveJsonDecoder.gen[TwitterStreamResponse]
