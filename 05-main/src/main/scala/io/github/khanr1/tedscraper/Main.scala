package io.github.khanr1
package tedscraper

import cats.effect.{IO, IOApp}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

/** Application entry point wiring logging and delegating to [[Program]]. */
object Main extends IOApp.Simple:
  given logger: Logger[IO] = Slf4jLogger.getLogger

  override def run: IO[Unit] = Program.make[IO]
