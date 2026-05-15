package io.github.khanr1.tedscraper.common

import types.*

case class TitleTranslation(
    language: Language,
    country: Option[RichText],
    town: Option[TownName],
    text: Option[RichText]
)
