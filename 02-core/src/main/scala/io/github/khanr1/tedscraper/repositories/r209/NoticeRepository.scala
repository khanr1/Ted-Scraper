package io.github.khanr1.tedscraper
package repositories.r209

trait NoticeRepository[F[_]] {
  def getAll: fs2.Stream[F, r209.Notice]
}
