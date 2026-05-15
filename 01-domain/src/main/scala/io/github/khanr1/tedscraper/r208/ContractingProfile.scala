package io.github.khanr1.tedscraper.r208

import io.github.khanr1.tedscraper.common.types.*
import io.github.khanr1.tedscraper.common.RichText

/** CA_CE_CONCESSIONAIRE_PROFILE (common.xsd). ORGANISATION, ADDRESS, TOWN,
  * COUNTRY required — typed non-Option.
  */
case class ContractingProfile(
    officialName: OfficialName,
    address: StreetAddress,
    town: TownName,
    postalCode: Option[PostalCode],
    country: CountryCode,
    contactPoint: Option[RichText],
    attention: Option[RichText],
    phone: Option[PhoneNumber],
    emails: List[EmailAddress],
    fax: Option[FaxNumber]
)
