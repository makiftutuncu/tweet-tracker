package dev.akif.tweettracker.models

enum TwitterError(val message: String, val details: String):
  case CreateRuleRequestFailed(override val details: String) extends TwitterError("Request to create rule failed", details)

  case StreamRequestFailed(override val details: String) extends TwitterError("Request to stream tweets failed", details)

  case CannotParseTweet(why: String, json: String) extends TwitterError(s"Cannot parse tweet", s"$why, payload was: $json")

  val log: String = s"$message: $details"
