package io.github.khanr1.tedscraper.r208
package types

// Source: EDITION attribute pattern in t_ted_export (TED_EXPORT.xd)
opaque type Edition = String
object Edition:
  private val R = raw"20[0-9]{2}[0-9][0-9]{0,2}".r
  def from(s: String): Either[String, Edition] =
    if R.matches(s) then Right(s)
    else Left(s"Invalid Edition: '$s'  (expected yyyynnn)")
  def unsafe(s: String): Edition = s
  extension (e: Edition) def value: String = e
