package io.github.khanr1.tedscraper.r208

import types.*

case class TransliteratedAddress(
    organisation: Option[OfficialName],
    address: StreetAddress,
    town: TownName,
    postalCode: Option[PostalCode],
    country: CountryCode,
    contactPoint: Option[RichText],
    attention: Option[RichText],
    phone: Option[PhoneNumber],
    email: Option[EmailAddress],
    fax: Option[FaxNumber]
)
