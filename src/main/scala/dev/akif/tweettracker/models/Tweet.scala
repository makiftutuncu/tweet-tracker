package dev.akif.tweettracker.models

import zio.json.*

import java.nio.charset.StandardCharsets

final case class Tweet(id: String, text: String, createdAt: Long)

object Tweet:
  given tweetEncoder: JsonEncoder[Tweet] = DeriveJsonEncoder.gen[Tweet]
