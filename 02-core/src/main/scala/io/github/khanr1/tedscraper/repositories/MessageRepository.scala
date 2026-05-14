package io.github.khanr1
package tedscraper
package repositories

import io.github.khanr1.tedscraper.Message

/** Algebra describing how to retrieve application messages in effect `F`. */
trait MessageRepository[F[_]] {
  /** Loads the greeting message from the underlying data source. */
  def getMessage: F[Message]
}
