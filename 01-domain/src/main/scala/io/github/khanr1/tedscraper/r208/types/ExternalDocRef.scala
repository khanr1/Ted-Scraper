package io.github.khanr1.tedscraper.r208
package types

// Source: NO_DOC_EXT element (type=DOCUMENTref = xs:string) — common.xsd
opaque type ExternalDocRef = String
object ExternalDocRef:
  def apply(s: String): ExternalDocRef = s
  extension (e: ExternalDocRef) def value: String = e
