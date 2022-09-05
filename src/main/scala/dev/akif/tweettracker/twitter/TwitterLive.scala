package dev.akif.tweettracker.twitter

import dev.akif.tweettracker.twitter.TwitterLive.*
import dev.akif.tweettracker.models.{TweetWithUser, Tweets, TwitterError, User}
import dev.akif.tweettracker.twitter.models.{TwitterRuleResponse, TwitterStreamResponse}
import dev.akif.tweettracker.Config
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.ziojson.*
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.{Header, MediaType, Uri}
import sttp.model.Uri.QuerySegment
import zio.json.*
import zio.stream.{ZSink, ZStream}
import zio.{System as _, *}

import java.nio.charset.StandardCharsets
import java.time.Instant

final case class TwitterLive(sttp: SttpBackend[Task, ZioStreams], config: Config) extends Twitter:
  override def streamTweets: IO[TwitterError, Tweets] =
    val searchTerm = config.searchTerm
    val forSeconds = config.forSeconds
    val upToTweets = config.upToTweets

    for
      _      <- ZIO.logInfo(s"Creating a streaming rule for search term '$searchTerm'")
      ruleId <- createRule(searchTerm)

      _     <- ZIO.logInfo(s"Streaming tweets containing '$searchTerm' for $forSeconds seconds or up to $upToTweets tweets")
      bytes <- httpStream
      tweetStream = tweetsFromBytes(bytes, ruleId)

      startedAt <- ZIO.clockWith(_.instant)
      deadline = startedAt.plusSeconds(forSeconds)
      tweetsSoFar <- Ref.make(Chunk.empty[TweetWithUser])

      limitedSink = ZSink.collectAllWhileZIO[Any, TwitterError, TweetWithUser] { tweet =>
        keepStreaming(tweetsSoFar, upToTweets, deadline).flatMap { keepStreaming =>
          if keepStreaming then tweetsSoFar.update(_ :+ tweet).as(keepStreaming) else ZIO.succeed(keepStreaming)
        }
      }

      rawTweets <- tweetStream.run(limitedSink)
      elapsed   <- ZIO.clockWith(c => c.instant).map(_.getEpochSecond - startedAt.getEpochSecond)

      _ <- ZIO.logInfo(s"Streamed ${rawTweets.size} tweets containing '$searchTerm' in $elapsed seconds")
    yield Tweets.from(rawTweets)

  private[twitter] def createRule(searchTerm: String): IO[TwitterError, String] =
    sttp
      .send(
        basicRequest
          .post(rulesUri)
          .auth
          .bearer(config.token)
          .header(Header.contentType(MediaType.ApplicationJson))
          .body(s"""{"add":[{"value":"$searchTerm","tag":"tweets containing $searchTerm"}]}""")
          .response(asJson[TwitterRuleResponse])
      )
      .flatMapError { cause =>
        val error = TwitterError.CreateRuleRequestFailed(cause.getMessage)
        ZIO.logErrorCause(error.log, Cause.fail(cause)).as(error)
      }
      .flatMap { response =>
        response.body match
          case Left(cause) =>
            val error = TwitterError.CreateRuleRequestFailed(cause.getMessage)
            ZIO.logErrorCause(error.log, Cause.fail(cause)) *> ZIO.fail(error)

          case Right(ruleResponse) =>
            ruleResponse.ruleIdFor(searchTerm)
      }

  private[twitter] def httpStream: IO[TwitterError, ZioStreams.BinaryStream] =
    sttp
      .send(
        basicRequest
          .get(streamUri)
          .auth
          .bearer(config.token)
          .response(asStreamUnsafe(ZioStreams))
      )
      .flatMapError { cause =>
        val error = TwitterError.StreamRequestFailed(cause.getMessage)
        ZIO.logErrorCause(error.log, Cause.fail(cause)).as(error)
      }
      .flatMap { response =>
        response.body match
          case Left(cause) =>
            val error = TwitterError.StreamRequestFailed(cause)
            ZIO.logErrorCause(error.log, Cause.fail(cause)) *> ZIO.fail(error)

          case Right(stream) =>
            ZIO.succeed(stream)
      }

object TwitterLive:
  val rulesUri: Uri =
    uri"https://api.twitter.com/2/tweets/search/stream/rules"

  val streamUri: Uri =
    uri"https://api.twitter.com/2/tweets/search/stream"
      .addQuerySegment(QuerySegment.KeyValue("tweet.fields", "created_at"))
      .addQuerySegment(QuerySegment.KeyValue("expansions", "author_id"))
      .addQuerySegment(QuerySegment.KeyValue("user.fields", "created_at"))

  val delimiter: Array[Byte] = "\r\n".getBytes(StandardCharsets.UTF_8)

  private[twitter] def keepStreaming(tweetsSoFar: Ref[Chunk[TweetWithUser]], upToTweets: Int, deadline: Instant): UIO[Boolean] =
    for
      now    <- ZIO.clockWith(_.instant)
      tweets <- tweetsSoFar.get
    yield tweets.size < upToTweets && now.isBefore(deadline)

  private[twitter] def tweetsFromBytes(
    byteStream: ZStream[Any, Throwable, Byte],
    ruleId: String
  ): ZStream[Any, TwitterError, TweetWithUser] =
    byteStream
      .mapError(cause => TwitterError.StreamRequestFailed(cause.getMessage))
      .mapAccumZIO[Any, TwitterError, Chunk[Byte], Chunk[TweetWithUser]](Chunk.empty) { (bytesSoFar, byte) =>
        if bytesSoFar.lastOption.contains(delimiter.head) && byte == delimiter.last then
          // In here, last byte in `bytesSoFar` would be "\r" and `byte` would be "\n".
          // So parse tweet from accumulated bytes except for the last one.
          // Also don't accumulate the current `byte` to skip the delimiter.
          parseTweet(bytesSoFar.dropRight(1), ruleId).map { maybeTweetWithUser =>
            Chunk.empty[Byte] -> maybeTweetWithUser.fold(Chunk.empty[TweetWithUser])(tweetWithUser => Chunk.single(tweetWithUser))
          }
        else
          // Keep accumulating bytes until we find the delimiter
          ZIO.succeed((bytesSoFar :+ byte) -> Chunk.empty[TweetWithUser])
      }
      .flatMap(tweets => ZStream.fromChunk(tweets))

  private[twitter] def parseTweet(bytes: Chunk[Byte], ruleId: String): IO[TwitterError, Option[TweetWithUser]] =
    val json = String(bytes.toArray, StandardCharsets.UTF_8)

    for
      _ <- ZIO.logTrace(s"Received json: $json")

      maybeTweetWithUser <-
        if json.isBlank then ZIO.none
        else
          ZIO
            .fromEither(json.fromJson[TwitterStreamResponse])
            .flatMapError { cause =>
              val error = TwitterError.CannotParseTweet(cause, json)
              ZIO.logErrorCause(error.log, Cause.fail(error)).as(error)
            }
            .flatMap(_.toTweetWithUserFor(ruleId))
    yield maybeTweetWithUser
