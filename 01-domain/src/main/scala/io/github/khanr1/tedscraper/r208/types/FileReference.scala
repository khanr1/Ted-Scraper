package io.github.khanr1.tedscraper.r208
package types

opaque type FileReference = String
object FileReference:
  def apply(s: String): FileReference = s
  extension (f: FileReference) def value: String = f
