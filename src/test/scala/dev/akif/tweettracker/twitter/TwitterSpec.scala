package dev.akif.tweettracker.twitter

import dev.akif.tweettracker.TestHelpers.*
import dev.akif.tweettracker.Config
import dev.akif.tweettracker.models.{TweetWithUser, Tweets, Tweet, TwitterError, User}
import dev.akif.tweettracker.twitter.models.{TwitterRule, TwitterRuleResponse}
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.impl.zio.RIOMonadAsyncError
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import zio.stream.ZStream
import zio.{Chunk, RIO, Ref, Runtime, Task, ZIO}
import zio.test.{Spec, ZIOSpecDefault, assertTrue}
import java.time.Instant

object TwitterSpec extends ZIOSpecDefault:
  val sttpStub: SttpBackendStub[Task, ZioStreams] = SttpBackendStub(RIOMonadAsyncError[Any]())
  val config: Config                              = Config("test", 3, 3, "test")

  given showErrorString: ShowError[String] = (s: String) => s

  val streamTweetsSpec: Spec[Any, TwitterError] =
    test("stream tweets") {
      val delimiter = String(TwitterLive.delimiter)
      val json =
        s"""
           |{"data":{"id":"1","author_id":"1","text":"Test 1","created_at":"1970-01-01T00:00:00Z"},"includes":{"users":[{"id":"1","name":"User 1","username":"user1","created_at":"1970-01-01T00:00:00Z"}]},"matching_rules":[{"id":"1"}]}
           |$delimiter
           |{"data":{"id":"2","author_id":"2","text":"Test 2","created_at":"1970-01-01T00:00:00Z"},"includes":{"users":[{"id":"2","name":"User 2","username":"user2","created_at":"1970-01-01T00:00:01Z"}]},"matching_rules":[{"id":"1"}]}
           |$delimiter
           |{"data":{"id":"3","author_id":"3","text":"Test 3","created_at":"1970-01-01T00:00:00Z"},"includes":{"users":[{"id":"3","name":"User 3","username":"user3","created_at":"1970-01-01T00:00:00Z"}]},"matching_rules":[{"id":"2"}]}
           |$delimiter
           |{"data":{"id":"4","author_id":"1","text":"Test 4","created_at":"1970-01-01T00:00:01Z"},"includes":{"users":[{"id":"1","name":"User 1","username":"user1","created_at":"1970-01-01T00:00:00Z"}]},"matching_rules":[{"id":"1"}]}
           |$delimiter
           |{"data":{"id":"5","author_id":"2","text":"Test 5","created_at":"1970-01-01T00:00:01Z"},"includes":{"users":[{"id":"2","name":"User 2","username":"user2","created_at":"1970-01-01T00:00:01Z"}]},"matching_rules":[{"id":"1"}]}
           |$delimiter
           |""".stripMargin

      val sttp =
        sttpStub
          .whenRequestMatches(_.uri == TwitterLive.rulesUri)
          .thenRespond[Either[ResponseException[String, String], TwitterRuleResponse]](
            Right(TwitterRuleResponse(Some(List(TwitterRule("1", "test"))), None))
          )
          .whenRequestMatches(_.uri == TwitterLive.streamUri)
          .thenRespond(SttpBackendStub.RawStream(ZStream.fromChunk(json.bytes)))

      val expected = Tweets(
        List(User("1", "User 1", "user1", 0L), User("2", "User 2", "user2", 1000L)),
        Map("user1" -> List(Tweet("1", "Test 1", 0L), Tweet("4", "Test 4", 1000L)), "user2" -> List(Tweet("2", "Test 2", 0L)))
      )

      TwitterLive(sttp, config).streamTweets.assertEquals(expected)
    }

  val createRuleSuite: Spec[Any, TwitterError] =
    suite("creating rule for given search term on Twitter")(
      test("fails when sending request fails") {
        val sttp =
          sttpStub
            .whenRequestMatches(_.uri == TwitterLive.rulesUri)
            .thenRespondF(ZIO.fail(Exception("request-failed")))

        TwitterLive(sttp, config)
          .createRule("test")
          .assertFails(TwitterError.CreateRuleRequestFailed("request-failed"))
      },
      test("fails when processing response body fails") {
        val error = DeserializationException("invalid", "deserialization-failed")

        val sttp =
          sttpStub
            .whenRequestMatches(_.uri == TwitterLive.rulesUri)
            .thenRespond[Either[ResponseException[String, String], TwitterRuleResponse]](Left(error))

        TwitterLive(sttp, config)
          .createRule("test")
          .assertFails(TwitterError.CreateRuleRequestFailed(error.getMessage))
      },
      test("fails when rule id cannot be parsed from the response") {
        val sttp =
          sttpStub
            .whenRequestMatches(_.uri == TwitterLive.rulesUri)
            .thenRespond[Either[ResponseException[String, String], TwitterRuleResponse]](Right(TwitterRuleResponse(None, None)))

        TwitterLive(sttp, config)
          .createRule("test")
          .assertFails(TwitterError.CreateRuleRequestFailed("no rule id found for search term 'test'"))
      },
      test("succeeds with created rule id") {
        val sttp =
          sttpStub
            .whenRequestMatches(_.uri == TwitterLive.rulesUri)
            .thenRespond[Either[ResponseException[String, String], TwitterRuleResponse]](
              Right(TwitterRuleResponse(Some(List(TwitterRule("1", "test"))), None))
            )

        TwitterLive(sttp, config)
          .createRule("test")
          .assertEquals("1")
      }
    )

  val httpStreamSuite: Spec[Any, TwitterError] =
    suite("getting an http stream for tweets from Twitter")(
      test("fails when sending request fails") {
        val sttp =
          sttpStub
            .whenRequestMatches(_.uri == TwitterLive.streamUri)
            .thenRespondF(ZIO.fail(Exception("request-failed")))

        TwitterLive(sttp, config).httpStream
          .assertFails(TwitterError.StreamRequestFailed("request-failed"))
      },
      test("fails when processing response body fails") {
        val sttp =
          sttpStub
            .whenRequestMatches(_.uri == TwitterLive.streamUri)
            .thenRespond[Either[String, ZioStreams]](Left("body-failed"))

        TwitterLive(sttp, config).httpStream
          .assertFails(TwitterError.StreamRequestFailed("body-failed"))
      },
      test("succeeds with a byte stream") {
        val sttp =
          sttpStub
            .whenRequestMatches(_.uri == TwitterLive.streamUri)
            .thenRespond(SttpBackendStub.RawStream(ZStream.fromChunk("{}".bytes)))

        TwitterLive(sttp, config).httpStream
          .flatMap(_.runCollect.orDie)
          .assertEquals("{}".bytes)
      }
    )

  val keepStreamingSuite: Spec[Any, TwitterError] =
    val tweetWithUser1 = TweetWithUser("1", User("1", "User 1", "user1", 1L), "Test", 1L)
    val tweetWithUser2 = TweetWithUser("2", User("2", "User 2", "user2", 2L), "Test", 2L)

    suite("checking if should keep streaming")(
      test("returns false when collected tweets so far reached the limit") {
        for
          tweetsSoFar <- Ref.make(Chunk(tweetWithUser1, tweetWithUser2))
          deadline    <- ZIO.clockWith(_.instant).map(_.plusSeconds(10))
          assertion   <- TwitterLive.keepStreaming(tweetsSoFar, 1, deadline).assertEquals(false)
        yield assertion
      },
      test("returns false when deadline is reached") {
        for
          tweetsSoFar <- Ref.make(Chunk(tweetWithUser1))
          deadline    <- ZIO.clockWith(_.instant)
          assertion   <- TwitterLive.keepStreaming(tweetsSoFar, 10, deadline).assertEquals(false)
        yield assertion
      },
      test("returns true when collected tweets so far have not reached the limit and deadline is not reached") {
        for
          tweetsSoFar <- Ref.make(Chunk(tweetWithUser1))
          deadline    <- ZIO.clockWith(_.instant).map(_.plusSeconds(10))
          assertion   <- TwitterLive.keepStreaming(tweetsSoFar, 10, deadline).assertEquals(true)
        yield assertion
      }
    )

  val tweetsFromBytesSuite: Spec[Any, TwitterError] =
    suite("getting tweets from bytes")(
      test("fails when underlying byte stream fails") {
        TwitterLive
          .tweetsFromBytes(ZStream.fail(Exception("test")), "1")
          .runCollect
          .assertFails(TwitterError.StreamRequestFailed("test"))
      },
      test("returns empty stream when underlying byte stream terminates with no tweet parsed successfully") {
        TwitterLive
          .tweetsFromBytes(ZStream.fromChunk("test".bytes), "1")
          .runCollect
          .assertThat(_.isEmpty)
      },
      test("fails when parsing a tweet fails") {
        val delimiter = String(TwitterLive.delimiter)
        val json      = s"""{}$delimiter"""

        TwitterLive
          .tweetsFromBytes(ZStream.fromChunk(json.bytes), "1")
          .runCollect
          .assertFails(TwitterError.CannotParseTweet(".data(missing)", "{}"))
      },
      test("returns parsed tweets matching given rule") {
        val delimiter = String(TwitterLive.delimiter)
        val json =
          s"""
             |{"data":{"id":"1","author_id":"1","text":"Test 1","created_at":"1970-01-01T00:00:00Z"},"includes":{"users":[{"id":"1","name":"User 1","username":"user1","created_at":"1970-01-01T00:00:00Z"}]},"matching_rules":[{"id":"1"}]}
             |$delimiter
             |{"data":{"id":"2","author_id":"2","text":"Test 2","created_at":"1970-01-01T00:00:00Z"},"includes":{"users":[{"id":"2","name":"User 2","username":"user2","created_at":"1970-01-01T00:00:00Z"}]},"matching_rules":[{"id":"1"}]}
             |$delimiter
             |{"data":{"id":"3","author_id":"3","text":"Test 3","created_at":"1970-01-01T00:00:00Z"},"includes":{"users":[{"id":"3","name":"User 3","username":"user3","created_at":"1970-01-01T00:00:00Z"}]},"matching_rules":[{"id":"2"}]}
             |$delimiter""".stripMargin

        TwitterLive
          .tweetsFromBytes(ZStream.fromChunk(json.bytes), "1")
          .runCollect
          .assertEquals(
            Chunk(
              TweetWithUser("1", User("1", "User 1", "user1", 0L), "Test 1", 0L),
              TweetWithUser("2", User("2", "User 2", "user2", 0L), "Test 2", 0L)
            )
          )
      }
    )

  val parseTweetSuite: Spec[Any, TwitterError] =
    suite("parsing tweet from chunk of bytes and a rule")(
      test("returns None when Json string is blank") {
        for
          assertion1 <- TwitterLive.parseTweet(Chunk.empty[Byte], "1").assertThat(_.isEmpty)
          assertion2 <- TwitterLive.parseTweet(" ".bytes, "1").assertThat(_.isEmpty)
        yield assertion1 && assertion2
      },
      test("fails when Json string is invalid") {
        TwitterLive.parseTweet("{}".bytes, "1").assertFails(TwitterError.CannotParseTweet(".data(missing)", "{}"))
      },
      test("returns None when tweet doesn't match to rule") {
        val json =
          """{"data":{"id":"1","author_id":"1","text":"Test","created_at":"1970-01-01T00:00:00Z"},"includes":{"users":[{"id":"1","name":"User","username":"user","created_at":"1970-01-01T00:00:00Z"}]},"matching_rules":[{"id":"1"}]}"""

        TwitterLive.parseTweet(json.bytes, "2").assertThat(_.isEmpty)
      },
      test("returns tweet with user as Some") {
        val json =
          """{"data":{"id":"1","author_id":"1","text":"Test","created_at":"1970-01-01T00:00:00Z"},"includes":{"users":[{"id":"1","name":"User","username":"user","created_at":"1970-01-01T00:00:00Z"}]},"matching_rules":[{"id":"1"}]}"""

        val expected = TweetWithUser("1", User("1", "User", "user", 0L), "Test", 0L)

        TwitterLive.parseTweet(json.bytes, "1").assertThat(_.contains(expected))
      }
    )

  override val spec: Spec[Any, TwitterError] =
    suite("TwitterSpec")(streamTweetsSpec, createRuleSuite, httpStreamSuite, keepStreamingSuite, tweetsFromBytesSuite, parseTweetSuite)
      .provideLayer(Runtime.removeDefaultLoggers)
