package dev.akif.tweettracker.twitter

import dev.akif.tweettracker.twitter.TwitterLive
import dev.akif.tweettracker.twitter.Twitter.TwitterError
import dev.akif.tweettracker.models.{TweetWithUser, Tweets, User}
import dev.akif.tweettracker.twitter.models.TwitterResponse
import dev.akif.tweettracker.Config
import sttp.capabilities.zio.ZioStreams
import sttp.client3.*
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.Uri
import sttp.model.Uri.QuerySegment
import zio.json.*
import zio.stream.{ZSink, ZStream}
import zio.{System as _, *}

import java.nio.charset.StandardCharsets

final case class TwitterLive(sttp: SttpBackend[Task, ZioStreams], config: Config) extends Twitter:
  override def streamTweets: IO[TwitterError, Tweets] =
    val searchTerm = config.searchTerm
    val forSeconds = config.forSeconds
    val upToTweets = config.upToTweets

    for
      // TODO: Make sure a rule with search term is created before starting to stream tweets

      _ <- ZIO.logInfo(s"Streaming tweets containing '$searchTerm' for $forSeconds seconds or up to $upToTweets tweets")

      startedAt = System.currentTimeMillis
      deadline  = startedAt + (forSeconds * 1000L)

      bytes     <- getHttpStream
      rawTweets <- getTweetStream(bytes).runFoldWhile(Chunk.empty[TweetWithUser])(keepGettingTweets(_, upToTweets, deadline))(_ :+ _)

      _ <- ZIO.logInfo(
        s"Streamed ${rawTweets.size} tweets containing '$searchTerm' in ${(System.currentTimeMillis - startedAt) / 1000} seconds"
      )
    yield
      val (userSet, usernamesToUnsortedTweets) =
        rawTweets.foldLeft(Set.empty[User] -> Map.empty[String, List[TweetWithUser]]) { case ((users, usernamesToTweets), tweet) =>
          val newUsers            = users + tweet.user
          val newTweetsOfUser     = usernamesToTweets.getOrElse(tweet.user.username, List.empty) :+ tweet
          val newUsernameToTweets = usernamesToTweets + (tweet.user.username -> newTweetsOfUser)
          newUsers -> newUsernameToTweets
        }

      Tweets(
        users = userSet.toList.sortBy(_.createdAt),
        tweetsOfUsers = usernamesToUnsortedTweets.map { case (username, tweets) =>
          username -> tweets.sortBy(_.createdAt).map(_.toTweet)
        }
      )

  def getHttpStream: IO[TwitterError, ZioStreams.BinaryStream] =
    sttp
      .send(
        basicRequest
          .get(TwitterLive.streamUri)
          .auth
          .bearer(config.token)
          .response(asStreamUnsafe(ZioStreams))
      )
      .flatMapError { cause =>
        val error = TwitterError.RequestFailed(cause.getMessage)
        ZIO.logErrorCause(error.log, Cause.fail(cause)).as(error)
      }
      .flatMap { response =>
        response.body match
          case Left(cause) =>
            val error = TwitterError.RequestFailed(cause)
            ZIO.logErrorCause(error.log, Cause.fail(cause)) *> ZIO.fail(error)

          case Right(stream) =>
            ZIO.succeed(stream)
      }

  def getTweetStream(byteStream: ZStream[Any, Throwable, Byte]): ZStream[Any, TwitterError, TweetWithUser] =
    byteStream
      .mapError(cause => TwitterError.RequestFailed(cause.getMessage))
      .mapAccumZIO(Chunk.empty[Byte]) { (bytesSoFar, byte) =>
        if foundDelimiter(bytesSoFar, byte) then {
          // In here, last byte in `bytesSoFar` would be "\r" and `byte` would be "\n".
          // So parse tweet from accumulated bytes except for the last one.
          // Also don't accumulate the current `byte` to skip the delimiter.
          val tweetBytes = bytesSoFar.dropRight(1)
          parseTweet(tweetBytes).map(tweet => Chunk.empty[Byte] -> Chunk.single(tweet))
        } else
          // Keep accumulating bytes until we find the delimiter
          ZIO.succeed((bytesSoFar :+ byte) -> Chunk.empty[TweetWithUser])
      }
      .flatMap(tweets => ZStream.fromChunk(tweets))

  def foundDelimiter(bytesSoFar: Chunk[Byte], byte: Byte): Boolean =
    bytesSoFar.lastOption.fold(false) { lastByte =>
      String(Array(lastByte, byte), StandardCharsets.UTF_8) == TwitterLive.delimiter
    }

  def keepGettingTweets(tweets: Chunk[TweetWithUser], upToTweets: Int, deadline: Long): Boolean =
    tweets.size < upToTweets && System.currentTimeMillis < deadline

  // TODO: Make this return Option[TweetWithUser]
  def parseTweet(bytes: Chunk[Byte]): IO[TwitterError, TweetWithUser] =
    val json = String(bytes.toArray, StandardCharsets.UTF_8)

    for
      _ <- ZIO.logTrace(s"Received tweet: $json")

      tweetWithUser <- ZIO
        .fromEither(json.fromJson[TwitterResponse])
        .flatMapError { cause =>
          val error = TwitterError.CannotParseTweet(cause, json)
          ZIO.logErrorCause(error.log, Cause.fail(error)).as(error)
        }
        .flatMap { response =>
          // TODO: Filter if tweet matched to the rule we want (containing our search term)
          val tweet = response.toTweetWithUser
          ZIO.logDebug(tweet.toJson).as(tweet)
        }
    yield tweetWithUser

object TwitterLive:
  val streamUri: Uri =
    uri"https://api.twitter.com/2/tweets/search/stream"
      .addQuerySegment(QuerySegment.KeyValue("tweet.fields", "created_at"))
      .addQuerySegment(QuerySegment.KeyValue("expansions", "author_id"))
      .addQuerySegment(QuerySegment.KeyValue("user.fields", "created_at"))

  val delimiter: String = "\r\n"
