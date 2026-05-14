package io.github.khanr1
package tedscraper

import cats.effect.*
import cats.implicits.*
import com.comcast.ip4s.*
import org.http4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.defaults.Banner
import org.http4s.server.Server
import org.typelevel.log4cats.Logger

/** Wraps an HTTP server resource managed within effect `F`. */
trait HttpServer[F[_]] {
  /** Resource that, when acquired, starts serving HTTP requests. */
  val serve: Resource[F, Server]
}

object HttpServer:
  /** Builds an Ember-backed [[HttpServer]] for the provided HTTP API. */
  def make[F[_]: Async: Logger](api: HttpApp[F]): HttpServer[F] =
    new HttpServer[F] {
      override val serve: Resource[F, Server] =
        EmberServerBuilder
          .default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(api)
          .build
          .evalTap(showEmberBanner)
      /** Logs the friendly banner emitted after the server starts. */
      private def showEmberBanner(s: Server): F[Unit] =
        Logger[F].info(
          "\n" + Banner.mkString("\n") + "\nHTTP Server started at " + s.address
        )
    }
