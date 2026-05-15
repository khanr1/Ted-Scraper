package io.github.khanr1.tedscraper.common.types

enum Currency:
    case BGN, CHF, CYP, CZK, DKK, EEK, EUR, GBP, HRK, HUF,
         ISK, JPY, LTL, LVL, MKD, MTL, NOK, PLN, RON, SEK,
         SKK, TRY, USD
    case Unknown(raw: String)

object Currency:
    import Currency.*
    private val mapping: Map[String, Currency] = Map("BGN" -> BGN, "CHF" -> CHF, "CYP" -> CYP, "CZK" -> CZK, "DKK" -> DKK, "EEK" -> EEK, "EUR" -> EUR, "GBP" -> GBP, "HRK" -> HRK, "HUF" -> HUF, "ISK" -> ISK, "JPY" -> JPY, "LTL" -> LTL, "LVL" -> LVL, "MKD" -> MKD, "MTL" -> MTL, "NOK" -> NOK, "PLN" -> PLN, "RON" -> RON, "SEK" -> SEK, "SKK" -> SKK, "TRY" -> TRY, "USD" -> USD)
    private val reverseMapping: Map[Currency, String] = mapping.map(_.swap)
    def from(s: String): Currency = mapping.getOrElse(s, Unknown(s))