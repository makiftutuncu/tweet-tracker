package dev.akif.tweettracker.twitter.models

import dev.akif.tweettracker.TestHelpers.*
import dev.akif.tweettracker.models.{TweetWithUser, TwitterError, User}
import zio.Runtime
import zio.json.*
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

import java.time.Instant

object TwitterStreamResponseSpec extends ZIOSpecDefault:
  val decodeFromJsonSpec: Spec[Any, Nothing] =
    test("a TwitterStreamResponse can be decoded from Json") {
      val actual =
        s"""{"data":{"id":"1","author_id":"1","text":"Test","created_at":"1970-01-01T00:00:00Z"},"includes":{"users":[{"id":"1","name":"User","username":"user","created_at":"1970-01-01T00:00:00Z"}]},"matching_rules":[{"id":"1"}]}"""
          .fromJson[TwitterStreamResponse]

      val expected = TwitterStreamResponse(
        TwitterData("1", "1", "Test", Instant.EPOCH),
        TwitterIncludes(List(TwitterUser("1", "User", "user", Instant.EPOCH))),
        List(TwitterMatchingRule("1"))
      )

      assertTrue(actual == Right(expected))
    }

  val toTweetWithUserForSuite: Spec[Any, Nothing] =
    suite("converting a TwitterStreamResponse to a TweetWithUser for a rule")(
      test("returns None when tweet doesn't match the rule") {
        TwitterStreamResponse(
          TwitterData("1", "1", "Test", Instant.EPOCH),
          TwitterIncludes(List(TwitterUser("1", "User", "user", Instant.EPOCH))),
          List(TwitterMatchingRule("1"))
        ).toTweetWithUserFor("2").assertThat(_.isEmpty)
      },
      test("dies when tweet's author is not in the includes") {
        val expected = RuntimeException(s"Cannot find user '1' in users, received incorrect data from Twitter")

        TwitterStreamResponse(TwitterData("1", "1", "Test", Instant.EPOCH), TwitterIncludes(List.empty), List(TwitterMatchingRule("1")))
          .toTweetWithUserFor("1")
          .assertDies(expected)
      },
      test("returns a TweetWithUser when tweet matches the rule") {
        val expected = TweetWithUser("1", User("1", "User", "user", 0L), "Test", 0L)

        TwitterStreamResponse(
          TwitterData("1", "1", "Test", Instant.EPOCH),
          TwitterIncludes(List(TwitterUser("1", "User", "user", Instant.EPOCH))),
          List(TwitterMatchingRule("1"))
        ).toTweetWithUserFor("1").assertThat(_.contains(expected))
      }
    )

  override def spec: Spec[Any, TwitterError] =
    suite("TwitterStreamResponseSpec")(decodeFromJsonSpec, toTweetWithUserForSuite)
