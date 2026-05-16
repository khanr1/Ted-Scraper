package io.github.khanr1
package tedscraper

import cats.Monad
import services.TedNoticeService
import repositories.r208.{NoticeRepository as R208Repo}
import repositories.r209.{NoticeRepository as R209Repo}
import repositories.eforms.EFormsNoticeRepository
import cats.effect.kernel.Async

/** Aggregates all high-level application services. */
sealed trait Services[F[_]] private (
    val tedNoticeService: TedNoticeService[F]
)

object Services {

  /** Instantiates [[Services]] wired with the provided repository dependencies.
    */
  def make[F[_]: Async](
      r208Repo: R208Repo[F],
      r209Repo: R209Repo[F],
      eformsRepo: EFormsNoticeRepository[F]
  ): Services[F] = new Services[F](
    tedNoticeService = TedNoticeService.make[F](r208Repo, r209Repo, eformsRepo)
  ) {}
}
