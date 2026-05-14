package io.github.khanr1
package tedscraper

import cats.Monad
import io.github.khanr1.tedscraper.services.MessageServices
import io.github.khanr1.tedscraper.repositories.MessageRepository

/** Aggregates all high-level application services. */
sealed trait Services[F[_]] private (
    val messages: MessageServices[F]
)

object Services {
  /** Instantiates [[Services]] wired with the provided repository dependencies. */
  def make[F[_]: Monad](
      messagesR: MessageRepository[F]
  ): Services[F] = new Services[F](
    messages = MessageServices.make[F](messagesR)
  ) {}
}
