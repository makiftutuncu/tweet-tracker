package dev.akif.tweettracker.twitter.models

import zio.json.*
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object TwitterRuleSpec extends ZIOSpecDefault:
  val decodeFromJsonSpec: Spec[Any, Nothing] =
    test("a TwitterRule can be decoded from Json") {
      val actual   = """{"id":"123","value":"test"}""".fromJson[TwitterRule]
      val expected = TwitterRule("123", "test")

      assertTrue(actual == Right(expected))
    }

  override def spec: Spec[Any, Nothing] =
    suite("TwitterRuleSpec")(decodeFromJsonSpec)
