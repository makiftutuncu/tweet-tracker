package dev.akif.tweettracker.twitter.models

import zio.json.*

import java.time.Instant

final case class TwitterUser(id: String, name: String, username: String, created_at: Instant)

object TwitterUser:
  given tweetUserDecoder: JsonDecoder[TwitterUser] = DeriveJsonDecoder.gen[TwitterUser]
