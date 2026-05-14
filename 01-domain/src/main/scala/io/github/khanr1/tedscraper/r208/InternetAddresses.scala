package io.github.khanr1.tedscraper.r208

import types.*

case class InternetAddresses(
    generalAddress: Option[IaUrl],
    buyerProfileAddress: Option[IaUrl],
    eTendering: Option[IaUrl]
)
