package io.github.khanr1.tedscraper.r208.types

enum ProcedureCode:
    case `1`, `2`, `3`, `4`, `5`, `6`, `7`, `8`, `9`
    case A, B, C, E, F, G, T, V, Z
    case Unknown(raw: String)

object ProcedureCode:
    private val mapping: Map[String, ProcedureCode] = Map("1" -> `1`, "2" -> `2`, "3" -> `3`, "4" -> `4`, "5" -> `5`, "6" -> `6`, "7" -> `7`, "8" -> `8`, "9" -> `9`) ++ Map("Open procedure" -> A, "Restricted procedure" -> B, "Competitive procedure with negotiation" -> C, "Competitive dialogue" -> E, "Innovation partnership" -> F, "Negotiated procedure without prior publication" -> G, "Negotiated procedure with prior publication" -> T, "Design contest" -> V, "Other" -> Z)
    private val reverseMapping: Map[ProcedureCode, String] = mapping.map(_.swap)
    def from(s: String): ProcedureCode = mapping.getOrElse(s, Unknown(s))