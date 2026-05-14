package io.github.khanr1.tedscraper.r208.types

enum TypeBidCode:
    case `1`  // Submission for all lots
    case `2`  // Submission for one lot only
    case `3`  // Submission for one or more lots
    case `8`  // Not applicable
    case `9`  // Not specified
    case Z    // Other
    case Unknown(raw: String)

object TypeBidCode:
    private val mapping: Map[String, TypeBidCode] = Map("Submission for all lots" -> `1`, "Submission for one lot only" -> `2`, "Submission for one or more lots" -> `3`, "Not applicable" -> `8`, "Not specified" -> `9`, "Other" -> Z)
    private val reverseMapping: Map[TypeBidCode, String] = mapping.map(_.swap)
    def from(s: String): TypeBidCode = mapping.getOrElse(s, Unknown(s))