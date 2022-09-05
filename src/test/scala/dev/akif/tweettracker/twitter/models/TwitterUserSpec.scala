package dev.akif.tweettracker.twitter.models

import zio.json.*
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

import java.time.Instant

object TwitterUserSpec extends ZIOSpecDefault:
  val decodeFromJsonSpec: Spec[Any, Nothing] =
    test("a TwitterUser can be decoded from Json") {
      val actual   = """{"id":"1","name":"User","username":"user","created_at":"1970-01-01T00:00:00Z"}""".fromJson[TwitterUser]
      val expected = TwitterUser("1", "User", "user", Instant.EPOCH)

      assertTrue(actual == Right(expected))
    }

  override def spec: Spec[Any, Nothing] =
    suite("TwitterUserSpec")(decodeFromJsonSpec)
