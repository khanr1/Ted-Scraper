package io.github.khanr1
package tedscraper

import cats.effect.kernel.Ref
import cats.effect.kernel.Sync
import cats.effect.kernel.Resource

/** Holds the primary application resources required at runtime. */
trait AppResource[F[_]] private (
)

object AppResource:
  /** Builds application resources backed by in-memory state. */
  def makeInMemory[F[_]: Sync](): Resource[F, AppResource[F]] = ???
