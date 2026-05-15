package io.github.khanr1.tedscraper.common
package types

opaque type CpvCode = String
object CpvCode:
  private val R = raw"\d{8}".r
  def from(s: String): Either[String, CpvCode] =
    if R.matches(s) then Right(s)
    else Left(s"Invalid CpvCode: '$s'  (expected 8 digits)")
  def unsafe(s: String): CpvCode = s
  extension (c: CpvCode) def value: String = c
