package dev.akif.tweettracker.twitter.models

import zio.json.*

import java.time.Instant

final case class TwitterData(id: String, author_id: String, text: String, created_at: Instant)

object TwitterData:
  given twitterDataDecoder: JsonDecoder[TwitterData] = DeriveJsonDecoder.gen[TwitterData]
