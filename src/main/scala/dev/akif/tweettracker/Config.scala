package dev.akif.tweettracker

import zio.{ULayer, ZLayer}

final case class Config(token: String)

object Config:
  lazy val live: ULayer[Config] =
    ZLayer.succeed(Config("token"))
