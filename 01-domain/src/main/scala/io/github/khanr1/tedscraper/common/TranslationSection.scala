package io.github.khanr1.tedscraper.common

case class TranslationSection(
    titles: List[TitleTranslation],
    authorityNames: List[AuthorityNameTranslation],
    transliterations: Option[TransliteratedAddress]
)
