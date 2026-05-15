package io.github.khanr1.tedscraper.r208

import io.github.khanr1.tedscraper.common.types.IaUrl

case class InternetAddresses(
    generalAddress: Option[IaUrl],
    buyerProfileAddress: Option[IaUrl],
    eTendering: Option[IaUrl]
)
