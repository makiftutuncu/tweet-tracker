package dev.akif.tweettracker.models

import zio.json.*
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object TweetSpec extends ZIOSpecDefault:
  val convertToJsonSpec: Spec[Any, Nothing] =
    test("a Tweet can be converted to Json") {
      val actual   = Tweet("123", "Hello world!", 456L).toJson
      val expected = """{"id":"123","text":"Hello world!","createdAt":456}"""

      assertTrue(actual == expected)
    }

  override def spec: Spec[Any, Nothing] =
    suite("TweetSpec")(convertToJsonSpec)
