package dev.akif.tweettracker.models

import zio.Chunk
import zio.json.*

final case class Tweets(users: List[User], tweetsOfUsers: Map[String, List[Tweet]])

object Tweets:
  given tweetsEncoder: JsonEncoder[Tweets] = DeriveJsonEncoder.gen[Tweets]

  def from(tweets: Chunk[TweetWithUser]): Tweets =
    val (userSet, usernamesToUnsortedTweets) =
      tweets.foldLeft(Set.empty[User] -> Map.empty[String, List[TweetWithUser]]) { case ((users, usernamesToTweets), tweet) =>
        val newUsers            = users + tweet.user
        val newTweetsOfUser     = usernamesToTweets.getOrElse(tweet.user.username, List.empty) :+ tweet
        val newUsernameToTweets = usernamesToTweets + (tweet.user.username -> newTweetsOfUser)
        newUsers -> newUsernameToTweets
      }

    val users = userSet.toList.sortBy(_.createdAt)

    val tweetsOfUsers = usernamesToUnsortedTweets.map { case (username, tweets) =>
      username -> tweets.sortBy(_.createdAt).map(_.toTweet)
    }

    Tweets(users, tweetsOfUsers)
