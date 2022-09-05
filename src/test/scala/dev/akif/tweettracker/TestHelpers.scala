package dev.akif.tweettracker

import zio.internal.stacktracer.SourceLocation
import zio.{Chunk, ZIO}
import zio.test.{TestResult, assertTrue}

import java.nio.charset.StandardCharsets

object TestHelpers:
  extension [R, E, A](effect: ZIO[R, E, A])
    def assertDies(expected: Throwable)(using SourceLocation): ZIO[R, Nothing, TestResult] =
      effect.exit
        .flatMap(_.cause)
        .map(_.defects.headOption)
        .map(defect => assertTrue(defect.exists(_.getMessage == expected.getMessage)))

    def assertFails(expected: E)(using SourceLocation): ZIO[R, Nothing, TestResult] =
      effect.exit
        .flatMap(_.cause)
        .map(_.failureOption)
        .map(error => assertTrue(error.contains(expected)))

    def assertThat(predicate: A => Boolean)(using SourceLocation): ZIO[R, E, TestResult] =
      effect.map(actual => assertTrue(predicate(actual)))

    def assertEquals(expected: A)(using SourceLocation): ZIO[R, E, TestResult] =
      effect.map(actual => assertTrue(actual == expected))

  extension (string: String)
    def bytes: Chunk[Byte] =
      Chunk(string.getBytes(StandardCharsets.UTF_8)*)
