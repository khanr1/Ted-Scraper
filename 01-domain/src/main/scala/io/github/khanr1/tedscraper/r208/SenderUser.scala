package io.github.khanr1.tedscraper.r208

import types.*

case class SenderUser(
    organisation: Option[OfficialName],
    attention: Option[RichText],
    address: Option[StreetAddress],
    postalCode: Option[PostalCode],
    town: Option[TownName],
    country: Option[CountryCode],
    phone: Option[PhoneNumber],
    fax: Option[FaxNumber],
    email: Option[EmailAddress],
    url: Option[Url]
)
