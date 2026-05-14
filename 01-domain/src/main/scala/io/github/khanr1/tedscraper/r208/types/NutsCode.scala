package io.github.khanr1.tedscraper.r208
package types

// Source: NUTS/@CODE and n2016:NUTS/@CODE — nuts_codes.xsd / nuts_codes_2016.xsd
// 2013 codes; validated as 2–5 alphanumeric characters.
opaque type NutsCode = String
object NutsCode:
  private val R = raw"[A-Z0-9]{2,5}".r
  def from(s: String): Either[String, NutsCode] =
    if R.matches(s) then Right(s)
    else Left(s"Invalid NutsCode: '$s'  (expected 2-5 uppercase alphanumeric)")
  def unsafe(s: String): NutsCode = s
  extension (n: NutsCode) def value: String = n
