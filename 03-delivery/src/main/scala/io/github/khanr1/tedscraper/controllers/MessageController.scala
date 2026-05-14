package io.github.khanr1
package tedscraper
package controllers

import io.github.khanr1.tedscraper.services.MessageServices

import cats.effect.kernel.Async
import cats.syntax.all.*
import io.circe.Encoder
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.HttpRoutes
import scala.util.chaining.*
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router

/** Delivery-layer controller exposing the `/hello` endpoint. */
object MessageController {
  /** Creates a controller that returns the message provided by [[MessageServices]]. */
  def make[F[_]: Async](
      service: MessageServices[F]
  ): Controller[F] =
    new Controller[F] with Http4sDsl[F] {
      private val prefixPath = "/hello"
      private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
        case GET -> Root => sayHello
      }

      /** Handles `GET /hello` by retrieving the message and encoding it as JSON. */
      def sayHello =
        service.sayHello.flatMap(m => (m.value).asJson.pipe(j => Ok(j)))

      override val routes: HttpRoutes[F] = Router(
        prefixPath -> httpRoutes
      )

    }

}
