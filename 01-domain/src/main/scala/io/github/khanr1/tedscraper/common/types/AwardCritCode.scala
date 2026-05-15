package io.github.khanr1.tedscraper.common.types

enum AwardCritCode:
    case `1`  // Lowest price
    case `2`  // Most economically advantageous tender
    case `3`  // Not specified
    case `8`  // Not applicable
    case `9`  // Not specified (alternative)
    case Z    // Other
    case Unknown(raw: String)

object AwardCritCode:
    private val mapping: Map[String, AwardCritCode] = Map("Lowest price" -> `1`, "Most economically advantageous tender" -> `2`, "Not specified" -> `3`, "Not applicable" -> `8`, "Not specified (alternative)" -> `9`, "Other" -> Z)
    private val reverseMapping: Map[AwardCritCode, String] = mapping.map(_.swap)
    def from(s: String): AwardCritCode = mapping.getOrElse(s, Unknown(s))