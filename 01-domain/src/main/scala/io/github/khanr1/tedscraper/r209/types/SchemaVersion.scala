package io.github.khanr1.tedscraper.r209
package types

// Full VERSION attribute from TED_EXPORT root element, e.g. "R2.0.9.S03.E01".
opaque type SchemaVersion = String
object SchemaVersion:
  def from(s: String): SchemaVersion = s
  extension (sv: SchemaVersion) def value: String = sv
