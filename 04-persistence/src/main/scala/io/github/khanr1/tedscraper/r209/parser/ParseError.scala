package io.github.khanr1.tedscraper.r209.parser

sealed trait ParseError:
  def message: String

object ParseError:
  case class XmlError(file: String, cause: Throwable) extends ParseError:
    def message = s"XML error in '$file': ${cause.getMessage}"

  case class MissingField(field: String, context: String) extends ParseError:
    def message = s"Missing required field '$field' in $context"

  case class UnknownFormType(label: String) extends ParseError:
    def message = s"Unknown form type element: '$label'"
