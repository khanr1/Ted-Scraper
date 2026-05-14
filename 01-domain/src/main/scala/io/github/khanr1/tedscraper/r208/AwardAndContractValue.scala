package io.github.khanr1.tedscraper.r208

import types.*

case class AwardAndContractValue(
    item: Option[RichText],
    contractNo: Option[RichText],
    lotNumbers: List[RichText],
    contractTitle: Option[RichText],
    awardDate: Option[StructuredDate],
    offersReceived: Option[Int],
    winner: Option[ContactData],
    valueInfo: Option[ContractValueInformation],
    pricePaid: Option[ValueCost],
    subcontracted: Option[Boolean]
)
