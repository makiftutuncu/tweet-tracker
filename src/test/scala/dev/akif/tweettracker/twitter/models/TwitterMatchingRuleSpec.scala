package dev.akif.tweettracker.twitter.models

import zio.json.*
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object TwitterMatchingRuleSpec extends ZIOSpecDefault:
  val decodeFromJsonSpec: Spec[Any, Nothing] =
    test("a TwitterMatchingRule can be decoded from Json") {
      val actual   = """{"id":"123"}""".fromJson[TwitterMatchingRule]
      val expected = TwitterMatchingRule("123")

      assertTrue(actual == Right(expected))
    }

  override def spec: Spec[Any, Nothing] =
    suite("TwitterMatchingRuleSpec")(decodeFromJsonSpec)
