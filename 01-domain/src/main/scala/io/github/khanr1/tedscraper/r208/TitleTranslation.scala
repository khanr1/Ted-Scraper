package io.github.khanr1.tedscraper.r208

import types.*

case class TitleTranslation(
    language: Language,
    country: Option[RichText],
    town: Option[TownName],
    text: Option[RichText]
)
