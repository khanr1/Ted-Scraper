package io.github.khanr1.tedscraper.r208.types


enum OjClass:
    case Regular, Annex, Electronic, Unknown

object OjClass:
    private val mapping: Map[String, OjClass] = Map(
        "Regular" -> Regular,
        "Annex" -> Annex,
        "Electronic" -> Electronic
    )
    private val reverseMapping: Map[OjClass, String] = mapping.map(_.swap)
    def from(s: String): OjClass = mapping.getOrElse(s, Unknown)
