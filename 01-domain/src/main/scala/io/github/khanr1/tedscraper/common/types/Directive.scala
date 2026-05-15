package io.github.khanr1.tedscraper.common.types

enum Directive:
    case Dir2007_66_EC  // Remedies directive
    case Dir2004_17_EC  // Utilities directive
    case Dir2004_18_EC  // Public sector directive
    case Dir2009_81_EC  // Defence directive
    case Reg1370_2007   // Public passenger transport regulation

object Directive:
    private val mapping: Map[String, Directive] = Map("Remedies directive" -> Dir2007_66_EC, "Utilities directive" -> Dir2004_17_EC, "Public sector directive" -> Dir2004_18_EC, "Defence directive" -> Dir2009_81_EC, "Public passenger transport regulation" -> Reg1370_2007)
    private val reverseMapping: Map[Directive, String] = mapping.map(_.swap)
    def from(s: String): Option[Directive] = mapping.get(s) 