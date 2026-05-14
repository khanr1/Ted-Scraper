package io.github.khanr1.tedscraper
package repositories.r208

trait NoticeRepository[F[_]] {
  def getAll: fs2.Stream[F, r208.Notice]
}
