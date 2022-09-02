package dev.akif.tweettracker.models

import zio.json.*

final case class Tweets(users: List[User], tweetsOfUsers: Map[String, List[Tweet]])

object Tweets:
  given tweetsEncoder: JsonEncoder[Tweets] = DeriveJsonEncoder.gen[Tweets]
