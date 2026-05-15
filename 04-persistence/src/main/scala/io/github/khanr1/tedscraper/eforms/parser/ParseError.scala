package io.github.khanr1.tedscraper.eforms.parser

sealed trait ParseError:
  def message: String

object ParseError:
  case class XmlLoadError(file: String, cause: Throwable) extends ParseError:
    def message = s"XML error in '$file': ${cause.getMessage}"

  case class MissingRequiredField(field: String, ctx: String) extends ParseError:
    def message = s"Missing required field '$field' in $ctx"

  case class InvalidFieldValue(field: String, raw: String, reason: String) extends ParseError:
    def message = s"Invalid value '$raw' for field '$field': $reason"

  case class UnknownNoticeSubtype(code: String) extends ParseError:
    def message = s"Unknown notice subtype: '$code'"

  case class UnknownDocumentType(rootElement: String) extends ParseError:
    def message = s"Unknown root element: '$rootElement'"
