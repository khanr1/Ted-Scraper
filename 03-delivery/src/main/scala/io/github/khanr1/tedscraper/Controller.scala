package io.github.khanr1
package tedscraper

import org.http4s.HttpRoutes

/** Base contract for delivery-layer controllers exposing HTTP routes. */
trait Controller[F[_]] {
  val routes: HttpRoutes[F]
}
