package io.github.khanr1.tedscraper.r208

import io.github.khanr1.tedscraper.common.types.NutsCode
import io.github.khanr1.tedscraper.common.RichText

case class LocationNuts(
    locationText: Option[RichText],
    nutsCodes: List[NutsCode]
)
