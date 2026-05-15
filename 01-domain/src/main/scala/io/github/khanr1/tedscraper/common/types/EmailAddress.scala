package io.github.khanr1.tedscraper.common.types

// Source: E_MAIL element (type=email = xs:string) — common.xsd
opaque type EmailAddress = String
object EmailAddress:
  def apply(s: String): EmailAddress = s
  extension (e: EmailAddress) def value: String = e
