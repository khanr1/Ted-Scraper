package io.github.khanr1.tedscraper
package console

import cats.effect.std.Console
import cats.effect.Async
import io.github.khanr1.tedscraper.services.TedNoticeService

trait ConsoleController[F[_]:Console:Async] {
  def run: F[Unit]
  
}


