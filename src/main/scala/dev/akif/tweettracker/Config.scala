package dev.akif.tweettracker

import zio.config.*
import zio.config.ConfigDescriptor.*
import zio.config.typesafe.*
import zio.{Duration, ULayer, ZLayer, ZIO}
import java.io.File

final case class Config(searchTerm: String, forSeconds: Int, upToTweets: Int, token: String)

object Config:
  private val searchTerm: ConfigDescriptor[String] = string("search-term").describe("The term to search in tweets")
  private val forSeconds: ConfigDescriptor[Int]    = int("for-seconds").describe("How long to stream tweets, in seconds")
  private val upToTweets: ConfigDescriptor[Int]    = int("up-to-tweets").describe("Maximum number of tweets to stream")
  private val token: ConfigDescriptor[String]      = string("token").describe("Twitter API v2 access token")

  private val descriptor: ConfigDescriptor[Config] = (searchTerm zip forSeconds zip upToTweets zip token).to[Config]

  val live: ZLayer[Any, ReadError[String], Config] =
    ZLayer(read(descriptor.from(TypesafeConfigSource.fromResourcePath)))
