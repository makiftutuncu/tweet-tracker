package dev.akif.tweettracker.twitter.models

import zio.json.*

final case class TwitterRuleError(id: String, title: String, value: String):
  def isDuplicateFor(searchTerm: String): Boolean =
    title == "DuplicateRule" && value == searchTerm

object TwitterRuleError:
  given twitterRuleErrorDecoder: JsonDecoder[TwitterRuleError] = DeriveJsonDecoder.gen[TwitterRuleError]
