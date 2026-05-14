package io.github.khanr1
package tedscraper

import cats.*
import cats.syntax.all.*
import org.http4s.HttpApp
import org.http4s.HttpRoutes
import org.http4s.server.middleware.RequestLogger
import org.http4s.server.middleware.ResponseLogger
import org.http4s.server.Router

/** Helpers for composing the HTTP API exposed by the delivery layer. */
object HttpApi:
  /** Builds an `HttpApp` by merging controller routes and enabling basic
    * logging.
    */
  def make[F[_]: effect.Async: Applicative](
      controllers: Controller[F]*
  ): org.http4s.HttpApp[F] =

    val logger: HttpApp[F] => HttpApp[F] = { (http: HttpApp[F]) =>
      RequestLogger.httpApp(true, true)(http)
    } andThen { (http: HttpApp[F]) =>
      ResponseLogger.httpApp(true, false)(http)
    }
    val bindedRoutes: HttpRoutes[F] =
      controllers
        .map(_.routes)
        .reduceLeft(_ <+> _)

    val routes: HttpRoutes[F] = Router(
      "/api" -> bindedRoutes
    )

    logger(routes.orNotFound)
