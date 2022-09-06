package dev.akif.tweettracker

import zio.{Cause, StackTrace, Trace, UIO, ZIO}

object ZIOExtentions:
  extension (t: Throwable)
    def log(message: => String)(using Trace): UIO[Unit] =
      for
        fiberId <- ZIO.fiberId
        cause = Cause.fail(t, StackTrace.fromJava(fiberId, t.getStackTrace))
        _ <- ZIO.logErrorCause(message, cause)
      yield ()
