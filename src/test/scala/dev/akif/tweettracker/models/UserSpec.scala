package dev.akif.tweettracker.models

import zio.json.*
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object UserSpec extends ZIOSpecDefault:
  val convertToJsonSpec: Spec[Any, Nothing] =
    test("user can be converted to Json") {
      val actual   = User("123", "John Doe", "john", 456L).toJson
      val expected = """{"id":"123","name":"John Doe","username":"john","createdAt":456}"""

      assertTrue(actual == expected)
    }

  override def spec: Spec[Any, Nothing] =
    suite("UserSpec")(convertToJsonSpec)
