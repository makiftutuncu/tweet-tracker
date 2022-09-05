package dev.akif.tweettracker.models

import zio.Chunk
import zio.json.*
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object TweetsSpec extends ZIOSpecDefault:
  val user1: User                   = User("1", "User 1", "user1", 1L)
  val user2: User                   = User("2", "User 2", "user2", 2L)
  val tweet1: Tweet                 = Tweet("1", "Tweet 1", 1L)
  val tweet2: Tweet                 = Tweet("2", "Tweet 2", 2L)
  val tweet3: Tweet                 = Tweet("3", "Tweet 3", 3L)
  val tweetWithUser1: TweetWithUser = TweetWithUser(tweet1.id, user1, tweet1.text, tweet1.createdAt)
  val tweetWithUser2: TweetWithUser = TweetWithUser(tweet2.id, user1, tweet2.text, tweet2.createdAt)
  val tweetWithUser3: TweetWithUser = TweetWithUser(tweet3.id, user2, tweet3.text, tweet3.createdAt)

  val convertToJsonSpec: Spec[Any, Nothing] =
    test("a Tweets can be converted to Json") {
      val actual = Tweets(List(user1, user2), Map(user1.username -> List(tweet1, tweet2), user2.username -> List(tweet3))).toJson

      val expected =
        """{"users":[{"id":"1","name":"User 1","username":"user1","createdAt":1},{"id":"2","name":"User 2","username":"user2","createdAt":2}],"tweetsOfUsers":{"user1":[{"id":"1","text":"Tweet 1","createdAt":1},{"id":"2","text":"Tweet 2","createdAt":2}],"user2":[{"id":"3","text":"Tweet 3","createdAt":3}]}}"""

      assertTrue(actual == expected)
    }

  val fromSpec: Spec[Any, Nothing] =
    test("a Tweets can be built from a chunk of TweetWithUser, sorting users and tweets by creation date in ascending order") {
      val actual   = Tweets.from(Chunk(tweetWithUser2, tweetWithUser1, tweetWithUser3))
      val expected = Tweets(List(user1, user2), Map(user1.username -> List(tweet1, tweet2), user2.username -> List(tweet3)))

      assertTrue(actual == expected)
    }

  override def spec: Spec[Any, Nothing] =
    suite("TweetsSpec")(convertToJsonSpec, fromSpec)
