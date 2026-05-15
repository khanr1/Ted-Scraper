package io.github.khanr1.tedscraper.r208

import io.github.khanr1.tedscraper.common.RichText

case class AwardOfContract(
    item: Option[RichText],
    contractNumber: Option[RichText],
    lotNumbers: List[RichText],
    contractTitle: Option[RichText],
    awardDate: Option[StructuredDate],
    offersReceived: Option[Int],
    offersReceivedMeaning: Option[Int],
    winner: Option[ContactData],
    contractValue: Option[ContractValueInformation],
    subcontracted: Option[Boolean]
)
