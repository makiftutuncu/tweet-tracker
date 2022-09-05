package dev.akif.tweettracker.twitter.models

import zio.json.*
import zio.test.{Spec, ZIOSpecDefault, assertTrue}

object TwitterRuleErrorSpec extends ZIOSpecDefault:
  val decodeFromJsonSpec: Spec[Any, Nothing] =
    test("a TwitterRuleError can be decoded from Json") {
      val actual   = """{"id":"123","title":"Test","value":"test"}""".fromJson[TwitterRuleError]
      val expected = TwitterRuleError("123", "Test", "test")

      assertTrue(actual == Right(expected))
    }

  val isDuplicateForSpec: Spec[Any, Nothing] =
    test("a TwitterRuleError can be checked to be duplicate of an existing rule") {
      val actual1 = TwitterRuleError("123", "Test", "test").isDuplicateFor("test")
      val actual2 = TwitterRuleError("123", "DuplicateRule", "test").isDuplicateFor("test2")
      val actual3 = TwitterRuleError("123", "DuplicateRule", "test").isDuplicateFor("test")

      assertTrue(!actual1) && assertTrue(!actual2) && assertTrue(actual3)
    }

  override def spec: Spec[Any, Nothing] =
    suite("TwitterRuleErrorSpec")(decodeFromJsonSpec, isDuplicateForSpec)
