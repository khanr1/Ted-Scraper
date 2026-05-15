package io.github.khanr1.tedscraper.common
package types

opaque type MonetaryAmount = BigDecimal
object MonetaryAmount:
  def from(bd: BigDecimal): Either[String, MonetaryAmount] =
    if bd >= 0 then Right(bd)
    else Left(s"MonetaryAmount must be non-negative, got: $bd")
  def fromString(s: String): Either[String, MonetaryAmount] =
    scala.util
      .Try(BigDecimal(s))
      .toEither
      .left
      .map(e => s"Cannot parse MonetaryAmount: '$s'  (${e.getMessage})")
      .flatMap(from)
  def unsafe(bd: BigDecimal): MonetaryAmount = bd
  extension (m: MonetaryAmount)
    def value: BigDecimal = m
    def toDouble: Double = m.toDouble
