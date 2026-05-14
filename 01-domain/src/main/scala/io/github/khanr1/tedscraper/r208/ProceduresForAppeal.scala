package io.github.khanr1.tedscraper.r208

case class ProceduresForAppeal(
    appealBody: Option[ContactData],
    mediationBody: Option[ContactData],
    lodgingAppeals: Option[RichText],
    lodgingInfoForService: Option[ContactData]
)
