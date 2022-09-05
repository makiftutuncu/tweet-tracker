package dev.akif.tweettracker.twitter.models

import zio.json.*

final case class TwitterRule(id: String, value: String)

object TwitterRule:
  given twitterRuleDecoder: JsonDecoder[TwitterRule] = DeriveJsonDecoder.gen[TwitterRule]
