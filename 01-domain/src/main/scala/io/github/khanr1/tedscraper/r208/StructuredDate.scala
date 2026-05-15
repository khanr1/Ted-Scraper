package io.github.khanr1.tedscraper.r208

import io.github.khanr1.tedscraper.common.RichText

// Not to be confused with TedDate (flat yyyymmdd from coded-data section).

case class StructuredDate(year: RichText, month: RichText, day: RichText):
  override def toString: String =
    s"${year.value}-${month.value.padStart(2, '0')}-${day.value.padStart(2, '0')}"

extension (s: String)
  private def padStart(n: Int, ch: Char): String =
    if s.length >= n then s else ch.toString * (n - s.length) + s
