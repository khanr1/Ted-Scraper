package io.github.khanr1.tedscraper
package services

import cats.effect.Async
import fs2.Stream

import io.github.khanr1.tedscraper.repositories.r208.{NoticeRepository as R208Repo}
import io.github.khanr1.tedscraper.repositories.r209.{NoticeRepository as R209Repo}
import io.github.khanr1.tedscraper.repositories.eforms.EFormsNoticeRepository
import io.github.khanr1.tedscraper.services.r208.r208Services
import io.github.khanr1.tedscraper.services.r209.r209Services
import io.github.khanr1.tedscraper.services.eforms.EFormsNoticeService

trait TedNoticeService[F[_]]:
  def toCSV: Stream[F, String]

object TedNoticeService:

  /** Aggregate all three format-specific repositories into a single CSV stream.
    *
    * One shared header is emitted (all three formats use an identical 34-column
    * schema), followed by data rows from r208, r209, and eforms in sequence.
    * Empty repositories contribute no rows but do not affect the header.
    */
  def make[F[_]: Async](
    r208Repo:   R208Repo[F],
    r209Repo:   R209Repo[F],
    eformsRepo: EFormsNoticeRepository[F]
  ): TedNoticeService[F] =
    new TedNoticeService[F]:
      private val r208Svc   = r208Services.make(r208Repo)
      private val r209Svc   = r209Services.make(r209Repo)
      private val eformsSvc = EFormsNoticeService.make(eformsRepo)

      def toCSV: Stream[F, String] =
        r208Svc.toCSV(r208Repo.getAll) ++
          r209Svc.toCSV(r209Repo.getAll).drop(1) ++
          eformsSvc.toCSV(eformsRepo.getAll).drop(1)
