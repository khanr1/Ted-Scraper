package io.github.khanr1.tedscraper.common
package types

// Source: OFFICIALNAME element (xs:string) — common.xsd
opaque type OfficialName = String
object OfficialName:
  def apply(s: String): OfficialName = s
  extension (n: OfficialName) def value: String = n
