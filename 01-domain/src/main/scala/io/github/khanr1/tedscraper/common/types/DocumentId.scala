package io.github.khanr1.tedscraper.common
package types

// Source: t_document_number in common_prod.xsd  (DOC_ID root attribute)
opaque type DocumentId = String
object DocumentId:
  private val R = raw"[0-9][0-9]{0,5}-20[0-9]{2}".r
  def from(s: String): Either[String, DocumentId] =
    if R.matches(s) then Right(s)
    else Left(s"Invalid DocumentId: '$s'")
  def unsafe(s: String): DocumentId = s
  extension (d: DocumentId) def value: String = d
