package io.github.khanr1.tedscraper
package repositories.eforms

import fs2.Stream
import io.github.khanr1.tedscraper.eforms.NoticeForm

trait EFormsNoticeRepository[F[_]]:
  def getAll: Stream[F, NoticeForm]
