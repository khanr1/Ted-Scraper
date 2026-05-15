package io.github.khanr1.tedscraper.common
package types

// Source: PHONE element (type=phone = xs:string) — common.xsd
opaque type PhoneNumber = String
object PhoneNumber:
  def apply(s: String): PhoneNumber = s
  extension (p: PhoneNumber) def value: String = p
