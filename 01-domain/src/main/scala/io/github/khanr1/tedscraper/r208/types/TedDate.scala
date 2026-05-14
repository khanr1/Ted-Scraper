package io.github.khanr1.tedscraper.r208
package types

// Source: t_simple_date in common_prod.xsd  (DS_DATE_DISPATCH, DATE_PUB, etc.)
opaque type TedDate = String
object TedDate:
  private val R =
    raw"20[0-9]{2}(1[0-2]|0[1-9])((3[0-1])|([1-2][0-9])|(0[1-9]))".r
  def from(s: String): Either[String, TedDate] =
    if R.matches(s) then Right(s)
    else Left(s"Invalid TedDate: '$s'  (expected yyyymmdd)")
  def unsafe(s: String): TedDate = s
  extension (d: TedDate)
    def value: String = d
    def year: String = d.take(4)
    def month: String = d.slice(4, 6)
    def day: String = d.slice(6, 8)
    def toIso: String = s"${d.take(4)}-${d.slice(4, 6)}-${d.slice(6, 8)}"
