package io.github.khanr1.tedscraper.r208
package types

// Source: URL element (type=url = xs:string) — common.xsd
// Note: IA_URL_GENERAL uses a stricter t_url_prod pattern;
// regular URL uses plain xs:string.
opaque type Url = String
object Url:
  def apply(s: String): Url = s
  extension (u: Url) def value: String = u
