package dev.akif.tweettracker.twitter.models

import zio.json.*

import java.time.Instant

final case class TwitterIncludes(users: List[TwitterUser])

object TwitterIncludes:
  given twitterIncludesDecoder: JsonDecoder[TwitterIncludes] = DeriveJsonDecoder.gen[TwitterIncludes]
