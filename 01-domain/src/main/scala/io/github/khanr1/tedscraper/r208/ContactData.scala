package io.github.khanr1.tedscraper.r208

import io.github.khanr1.tedscraper.common.types.*
import io.github.khanr1.tedscraper.common.RichText

case class ContactData(
    officialName: Option[OfficialName],
    address: Option[StreetAddress],
    town: Option[TownName],
    postalCode: Option[PostalCode],
    country: Option[CountryCode],
    contactPoint: Option[RichText],
    attention: Option[RichText],
    phone: Option[PhoneNumber],
    emails: List[EmailAddress],
    fax: Option[FaxNumber],
    url: Option[Url]
)
