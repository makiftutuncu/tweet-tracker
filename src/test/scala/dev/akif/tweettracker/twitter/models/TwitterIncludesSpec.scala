package dev.akif.tweettracker.twitter.models

import zio.json.*
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

import java.time.Instant

object TwitterIncludesSpec extends ZIOSpecDefault:
  val decodeFromJsonSpec: Spec[Any, Nothing] =
    test("a TwitterIncludes can be decoded from Json") {
      val actual =
        """{"users":[{"id":"123","name":"John Doe","username":"john","created_at":"1970-01-01T00:00:00Z"}]}""".fromJson[TwitterIncludes]

      val expected = TwitterIncludes(List(TwitterUser("123", "John Doe", "john", Instant.EPOCH)))

      assertTrue(actual == Right(expected))
    }

  override def spec: Spec[Any, Nothing] =
    suite("TwitterIncludesSpec")(decodeFromJsonSpec)
