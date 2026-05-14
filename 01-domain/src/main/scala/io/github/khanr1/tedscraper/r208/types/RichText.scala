package io.github.khanr1.tedscraper.r208.types

// Source: t_reception_id in common_prod.xsd
opaque type ReceptionId = String
object ReceptionId:
  private val R = raw"\d{2}-\d{6}-\d{3}".r
  def from(s: String): Either[String, ReceptionId] =
    if R.matches(s) then Right(s)
    else Left(s"Invalid ReceptionId: '$s'  (expected \\d{2}-\\d{6}-\\d{3})")
  def unsafe(s: String): ReceptionId = s
  extension (r: ReceptionId) def value: String = r
