package io.github.khanr1.tedscraper.r208

import io.github.khanr1.tedscraper.common.RichText

case class ProceduresForAppeal(
    appealBody: Option[ContactData],
    mediationBody: Option[ContactData],
    lodgingAppeals: Option[RichText],
    lodgingInfoForService: Option[ContactData]
)
