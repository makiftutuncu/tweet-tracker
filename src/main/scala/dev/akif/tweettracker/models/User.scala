package dev.akif.tweettracker.models

import zio.json.*

final case class User(id: String, name: String, username: String, createdAt: Long)

object User:
  given userEncoder: JsonEncoder[User] = DeriveJsonEncoder.gen[User]
