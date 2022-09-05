package dev.akif.tweettracker.models

import zio.json.*
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object TweetWithUserSpec extends ZIOSpecDefault:
  val user: User = User("1", "User", "user", 1L)

  val convertToJsonSpec: Spec[Any, Nothing] =
    test("a TweetWithUser can be converted to Json") {
      val actual = TweetWithUser("123", user, "Hello world!", 456L).toJson

      val expected =
        """{"id":"123","user":{"id":"1","name":"User","username":"user","createdAt":1},"text":"Hello world!","createdAt":456}"""

      assertTrue(actual == expected)
    }

  val toTweetSpec: Spec[Any, Nothing] =
    test("a TweetWithUser can be converted to Tweet") {
      val actual   = TweetWithUser("123", user, "Hello world!", 456L).toTweet
      val expected = Tweet("123", "Hello world!", 456L)

      assertTrue(actual == expected)
    }

  override def spec: Spec[Any, Nothing] =
    suite("TweetWithUserSpec")(convertToJsonSpec, toTweetSpec)
