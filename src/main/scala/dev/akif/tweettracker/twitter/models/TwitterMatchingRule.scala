package dev.akif.tweettracker.twitter.models

import zio.json.*

final case class TwitterMatchingRule(id: String)

object TwitterMatchingRule:
  given twitterMatchingRuleDecoder: JsonDecoder[TwitterMatchingRule] = DeriveJsonDecoder.gen[TwitterMatchingRule]
