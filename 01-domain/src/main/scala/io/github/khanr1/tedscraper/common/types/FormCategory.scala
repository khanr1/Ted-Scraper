package io.github.khanr1.tedscraper.common.types

enum FormCategory:
    case Original, Translation

object FormCategory:
    private val mapping = Map(
        "original" -> Original,
        "translation" -> Translation
    )

    def fromString(s: String): FormCategory = mapping.getOrElse(s, Original)
    def from(s: String): Option[FormCategory] = mapping.get(s.toLowerCase)