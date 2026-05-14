package io.github.khanr1.tedscraper.r208
package types

// 24 official EU languages.
  enum Language:
    case BG
    case CS
    case DA
    case DE
    case EL
    case EN
    case ES
    case ET
    case FI
    case FR
    case GA
    case HR
    case HU
    case IT
    case LT
    case LV
    case MT
    case NL
    case PL
    case PT
    case RO
    case SK
    case SL
    case SV
    case Unknown(raw: String)
 
  object Language:
    private val mapping: Map[String,Language] = Map(
      "BG" -> Language.BG,
      "CS" -> Language.CS,
      "DA" -> Language.DA,
      "DE" -> Language.DE,
      "EL" -> Language.EL,
      "EN" -> Language.EN,
      "ES" -> Language.ES,
      "ET" -> Language.ET,
      "FI" -> Language.FI,
      "FR" -> Language.FR,
      "GA" -> Language.GA,
      "HR" -> Language.HR,
      "HU" -> Language.HU,
      "IT" -> Language.IT,
      "LT" -> Language.LT,
      "LV" -> Language.LV,
      "MT" -> Language.MT,
      "NL" -> Language.NL,
      "PL" -> Language.PL,
      "PT" -> Language.PT,
      "RO" -> Language.RO,
      "SK" -> Language.SK,
      "SL" -> Language.SL,
      "SV" -> Language.SV
    )
    private val reverseMapping: Map[Language,String] = mapping.map(_.swap)
    def from(s: String): Language = mapping.getOrElse(s, Language.Unknown(s))
