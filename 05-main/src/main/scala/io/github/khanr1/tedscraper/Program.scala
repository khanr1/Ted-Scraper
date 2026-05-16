package io.github.khanr1
package tedscraper

import cats.effect.kernel.Async
import cats.syntax.all.*

/** Assembles the application graph and starts the HTTP server. */
object Program {

  /** Bootstraps all modules and runs the server until the effect is canceled.
    */
  def make[F[_]: Async: org.typelevel.log4cats.Logger]: F[Unit] = ???

}
