package io.github.khanr1
package tedscraper

import cats.effect.kernel.Ref
import cats.effect.kernel.Sync
import cats.effect.kernel.Resource
import io.github.khanr1.tedscraper.Message

/** Holds the primary application resources required at runtime. */
trait AppResource[F[_]] private (
    val messageRef: Ref[F, Message]
)

object AppResource:
  /** Builds application resources backed by in-memory state. */
  def makeInMemory[F[_]: Sync](): Resource[F, AppResource[F]] = {

    /** Allocates the mutable reference used to store the greeting message. */
    def mkMessage: Resource[F, Ref[F, Message]] =
      Resource.eval(Ref.of(Message("Hello World")))

    (mkMessage).map(
      new AppResource[F](_) {}
    )

  }
