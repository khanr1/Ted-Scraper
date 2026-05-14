package io.github.khanr1.tedscraper.r208
package types

// Source: t_no_doc_ojs in common_prod.xsd  (NO_DOC_OJS element)
opaque type NoticeNumber = String
object NoticeNumber:
  private val R = raw"20\d{2}/S \d{1,3}-\d{6}".r
  def from(s: String): Either[String, NoticeNumber] =
    if R.matches(s) then Right(s)
    else Left(s"Invalid NoticeNumber: '$s'  (expected 20YY/S NNN-NNNNNN)")
  def unsafe(s: String): NoticeNumber = s
  extension (n: NoticeNumber)
    def value: String = n
    def year: String = n.take(4)
    def ojNumber: String = n.drop(7).takeWhile(_ != '-')
    def sequenceNumber: String = n.dropWhile(_ != '-').tail
