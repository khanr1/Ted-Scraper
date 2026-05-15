package io.github.khanr1.tedscraper.common
package types

// Source: FAX element (type=fax = xs:string) — common.xsd
opaque type FaxNumber = String
object FaxNumber:
  def apply(s: String): FaxNumber = s
  extension (f: FaxNumber) def value: String = f
