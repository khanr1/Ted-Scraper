package io.github.khanr1.tedscraper.r208

case class TranslationSection(
    titles: List[TitleTranslation],
    authorityNames: List[AuthorityNameTranslation],
    transliterations: Option[TransliteratedAddress]
)
