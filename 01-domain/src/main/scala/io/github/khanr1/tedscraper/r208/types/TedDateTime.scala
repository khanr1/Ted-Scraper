package io.github.khanr1.tedscraper.r208
package types

opaque type TedDateTime = String
object TedDateTime:
  private val R =
    raw"20[0-9]{2}(1[0-2]|0[1-9])((3[0-1])|([1-2][0-9])|(0[1-9]))( ((2[0-3])|([0-1][0-9])):[0-5][0-9])?".r
  def from(s: String): Either[String, TedDateTime] =
    if R.matches(s) then Right(s)
    else Left(s"Invalid TedDateTime: '$s'")
  def unsafe(s: String): TedDateTime = s
  extension (d: TedDateTime)
    def value: String = d
    def datePart: String = d.take(8)
    def timePart: Option[String] =
      if d.length > 8 then Some(d.drop(9).trim) else None
