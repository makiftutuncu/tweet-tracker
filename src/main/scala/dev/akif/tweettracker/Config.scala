package dev.akif.tweettracker

import zio.{Duration, ULayer, ZLayer}

final case class Config(searchTerm: String, forSeconds: Int, upToTweets: Int, token: String)

object Config:
  lazy val live: ULayer[Config] =
    ZLayer.succeed(Config(searchTerm = "crypto", forSeconds = 30, upToTweets = 100, token = "token"))
