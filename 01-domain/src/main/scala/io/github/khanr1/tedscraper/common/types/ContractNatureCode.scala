package io.github.khanr1.tedscraper.common.types

enum ContractNatureCode:
    case `1`  // Works
    case `2`  // Supplies
    case `3`  // Services
    case `4`  // Mixed
    case `9`  // Not specified
    case Z    // Not applicable
    case Unknown(raw: String)

object ContractNatureCode:
    private val mapping: Map[String, ContractNatureCode] = Map("Works" -> `1`, "Supplies" -> `2`, "Services" -> `3`, "Mixed" -> `4`, "Not specified" -> `9`, "Not applicable" -> Z)
    private val reverseMapping: Map[ContractNatureCode, String] = mapping.map(_.swap)
    def from(s: String): ContractNatureCode = mapping.getOrElse(s, Unknown(s))