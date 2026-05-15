package io.github.khanr1.tedscraper.common

// Source: btx type (rich inline XML) — common.xsd
// We extract the concatenated text content; the opaque wrapper signals
// "this came from a structured text block, not a flat attribute".
opaque type RichText = String
object RichText:
  def apply(s: String): RichText = s
  extension (r: RichText) def value: String = r
