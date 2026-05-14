package io.github.khanr1
package tedscraper
package repositories

import io.github.khanr1.tedscraper.Message
import cats.Applicative
import cats.syntax.all.*
import cats.effect.kernel.Ref

/** In-memory implementation of [[MessageRepository]] backed by a `Ref`. */
object MessageInMemoryRepo {
  /** Creates a repository that reads a message from the supplied reference. */
  def make[F[_]: Applicative](repo: Ref[F, Message]): MessageRepository[F] =
    new MessageRepository[F] {

      override def getMessage: F[Message] = repo.get

    }
}
