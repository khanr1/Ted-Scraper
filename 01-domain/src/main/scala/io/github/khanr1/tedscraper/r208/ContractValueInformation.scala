package io.github.khanr1.tedscraper.r208

case class ContractValueInformation(
    estimatedValue: Option[FormContractValue],
    finalValue: Option[FormContractValue],
    durationMonths: Option[Int],
    durationYears: Option[Int]
)
