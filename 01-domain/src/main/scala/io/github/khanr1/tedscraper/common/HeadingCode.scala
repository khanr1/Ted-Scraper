package io.github.khanr1.tedscraper.common

// Source: HEADING element (xs:string) in CODIF_DATA — common_prod.xsd
opaque type HeadingCode = String
object HeadingCode:
  def apply(s: String): HeadingCode = s
  extension (h: HeadingCode) def value: String = h
