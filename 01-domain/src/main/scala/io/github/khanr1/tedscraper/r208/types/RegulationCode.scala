package io.github.khanr1.tedscraper.r208.types

enum RegulationCode:
    case `0`, `1`, `2`, `3`, `4`, `5`, `6`, `7`, `8`, `9`
    case A, B, C, S, Z
    case Unknown(raw: String)

object RegulationCode:
    private val mapping: Map[String, RegulationCode] = Map("0" -> `0`, "1" -> `1`, "2" -> `2`, "3" -> `3`, "4" -> `4`, "5" -> `5`, "6" -> `6`, "7" -> `7`, "8" -> `8`, "9" -> `9`) ++ Map("Directive 2014/24/EU" -> A, "Directive 2014/25/EU" -> B, "Directive 2014/23/EU" -> C, "Other" -> S, "Not specified" -> Z)
    private val reverseMapping: Map[RegulationCode, String] = mapping.map(_.swap)
    def from(s: String): RegulationCode = mapping.getOrElse(s, Unknown(s))