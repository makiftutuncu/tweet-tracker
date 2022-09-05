package dev.akif.tweettracker.twitter.models

import dev.akif.tweettracker.models.TwitterError
import dev.akif.tweettracker.TestHelpers.*
import zio.Runtime
import zio.json.*
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object TwitterRuleResponseSpec extends ZIOSpecDefault:
  val decodeFromJsonSpec: Spec[Any, Nothing] =
    test("a TwitterRule response can be decoded from Json") {
      val actual =
        s"""{"data":[{"id":"1","value":"test"}],"errors":[{"id":"1","title":"test","value":"test"}]}""".fromJson[TwitterRuleResponse]

      val expected = TwitterRuleResponse(Some(List(TwitterRule("1", "test"))), Some(List(TwitterRuleError("1", "test", "test"))))

      assertTrue(actual == Right(expected))
    }

  val ruleIdForSuite: Spec[Any, TwitterError] =
    suite("getting rule id from a TwitterRule response")(
      test("returns rule id from data when rule is created") {
        TwitterRuleResponse(Some(List(TwitterRule("1", "test"))), None)
          .ruleIdFor("test")
          .assertEquals("1")
      },
      test("returns rule id from errors when rule exists and listed as duplicate in errors") {
        TwitterRuleResponse(None, Some(List(TwitterRuleError("1", "DuplicateRule", "test"))))
          .ruleIdFor("test")
          .assertEquals("1")
      },
      test("fails when rule id is not found") {
        for
          assertion1 <- TwitterRuleResponse(None, None)
            .ruleIdFor("test")
            .assertFails(TwitterError.CreateRuleRequestFailed("no rule id found for search term 'test'"))

          assertion2 <- TwitterRuleResponse(Some(List(TwitterRule("1", "test"))), Some(List(TwitterRuleError("1", "test", "test"))))
            .ruleIdFor("test2")
            .assertFails(TwitterError.CreateRuleRequestFailed("no rule id found for search term 'test2'"))
        yield assertion1 && assertion2
      }
    )

  override def spec: Spec[Any, TwitterError] =
    suite("TwitterRuleResponseSpec")(decodeFromJsonSpec, ruleIdForSuite).provideLayer(Runtime.removeDefaultLoggers)
