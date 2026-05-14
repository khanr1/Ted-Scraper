package io.github.khanr1.tedscraper.r208.types

enum DocumentTypeCode:
    case `0`, `1`, `2`, `3`, `4`, `5`, `6`, `7`, `8`, `9`
    case A, B, C, D, E, F, G, H, I, J, K, M, O, P, Q, R, S, V, Y
    case Unknown(raw: String)

object DocumentTypeCode:
    private val mapping: Map[String, DocumentTypeCode] = Map("0" -> `0`, "1" -> `1`, "2" -> `2`, "3" -> `3`, "4" -> `4`, "5" -> `5`, "6" -> `6`, "7" -> `7`, "8" -> `8`, "9" -> `9`, "A" -> A, "B" -> B, "C" -> C, "D" -> D, "E" -> E, "F" -> F, "G" -> G, "H" -> H, "I" -> I, "J" -> J, "K" -> K, "M" -> M, "O" -> O, "P" -> P, "Q" -> Q, "R" -> R , "S" -> S, "V" -> V, "Y" -> Y)
    private val reverseMapping: Map[DocumentTypeCode, String] = mapping.map(_.swap)
    def from(s: String): DocumentTypeCode = mapping.getOrElse(s, Unknown(s))