package dev.akif.tweettracker.twitter.models

import dev.akif.tweettracker.models.TwitterError
import zio.*
import zio.json.*

final case class TwitterRuleResponse(data: Option[List[TwitterRule]], errors: Option[List[TwitterRuleError]]):
  def ruleIdFor(searchTerm: String): IO[TwitterError, String] =
    val createdId = data.flatMap {
      _.collectFirst {
        case rule if rule.value == searchTerm =>
          rule.id
      }
    }

    createdId match
      case Some(id) =>
        ZIO.logInfo(s"Rule for search term '$searchTerm' is created as '$id'").as(id)

      case None =>
        val existingId = errors.flatMap {
          _.collectFirst {
            case error if error.isDuplicateFor(searchTerm) =>
              error.id
          }
        }

        existingId match
          case Some(id) =>
            ZIO.logInfo(s"Rule for search term '$searchTerm' already exists as '$id'").as(id)

          case None =>
            val error = TwitterError.CreateRuleRequestFailed(s"no rule id found for search term '$searchTerm'")
            ZIO.logError(error.log) *> ZIO.fail(error)

object TwitterRuleResponse:
  given twitterRuleResponseDecoder: JsonDecoder[TwitterRuleResponse] = DeriveJsonDecoder.gen[TwitterRuleResponse]
