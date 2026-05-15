package io.github.khanr1.tedscraper.common
package types

// Source: ADDRESS element (type=address = xs:string) — common.xsd
opaque type StreetAddress = String
object StreetAddress:
  def apply(s: String): StreetAddress = s
  extension (a: StreetAddress) def value: String = a
