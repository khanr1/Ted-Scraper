package io.github.khanr1.tedscraper.common.types

// Source: TOWN element (xs:string) — common.xsd
opaque type TownName = String
object TownName:
  def apply(s: String): TownName = s
  extension (t: TownName) def value: String = t
