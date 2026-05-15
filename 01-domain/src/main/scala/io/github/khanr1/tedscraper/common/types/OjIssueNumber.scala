package io.github.khanr1.tedscraper.common
package types

opaque type OjIssueNumber = String
object OjIssueNumber:
  private val R = raw"[0-9]{1,3}".r
  def from(s: String): Either[String, OjIssueNumber] =
    if R.matches(s) then Right(s)
    else Left(s"Invalid OjIssueNumber: '$s'  (expected 1-3 digits)")
  def unsafe(s: String): OjIssueNumber = s
  extension (n: OjIssueNumber) def value: String = n
