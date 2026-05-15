package io.github.khanr1.tedscraper.common.types

// Source: POSTAL_CODE element (xs:string) — common.xsd
opaque type PostalCode = String
object PostalCode:
  def apply(s: String): PostalCode = s
  extension (p: PostalCode) def value: String = p
