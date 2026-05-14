package io.github.khanr1.tedscraper.r208
package types

// Source: IA_URL_GENERAL (type=t_url_prod with strict pattern) — common_prod.xsd
// Keeps URL and IaUrl distinct even though both are strings.
opaque type IaUrl = String
object IaUrl:
  private val R = """(((http|https)://)?([a-zA-Z]([a-zA-Z0-9\-]*))(\.([a-zA-Z0-9]([a-zA-Z0-9\-]*))*)\.[a-zA-Z]{1,10}(:[0-9]{1,5})?(/([a-zA-Z0-9_\-\.%])*)*((#|\?)([a-zA-Z0-9\?\./;:\-&#_=#%$])*)?)?""".r
  def from(s: String): Either[String, IaUrl] =
    if s.isEmpty || R.matches(s) then Right(s)
    else Left(s"Invalid IaUrl: '$s'")
  def unsafe(s: String): IaUrl = s
  extension (u: IaUrl) def value: String = u
