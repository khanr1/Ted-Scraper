package io.github.khanr1.tedscraper.common

// ── ValuesType  (VALUES/@TYPE attribute — common_prod.xsd) ──────────────────
enum ValuesType:
    case Global, PerLot, Contract

object ValuesType:
    private val mapping: Map[String, ValuesType] = Map("GLOBAL" -> Global, "PER_LOT" -> PerLot, "CONTRACT" -> Contract)
    private val reverseMapping: Map[ValuesType, String] = mapping.map(_.swap)
    def from(s: String): Either[String, ValuesType] = mapping.get(s).toRight(s"Invalid ValuesType: '$s'  (expected one of ${mapping.keys.mkString(", ")})")