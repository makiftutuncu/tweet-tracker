package dev.akif.tweettracker.models

import zio.json.*
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object TwitterErrorSpec extends ZIOSpecDefault:
  val logMessageSpec: Spec[Any, Nothing] =
    test("a TwitterError can produce a log message with details") {
      val actual1   = TwitterError.CreateRuleRequestFailed("test").log
      val expected1 = "Request to create rule failed: test"

      val actual2   = TwitterError.StreamRequestFailed("test").log
      val expected2 = "Request to stream tweets failed: test"

      val actual3   = TwitterError.CannotParseTweet("test", "{}").log
      val expected3 = "Cannot parse tweet: test, payload was: {}"

      assertTrue(actual1 == expected1) && assertTrue(actual2 == expected2) && assertTrue(actual3 == expected3)
    }

  override def spec: Spec[Any, Nothing] =
    suite("TwitterErrorSpec")(logMessageSpec)
