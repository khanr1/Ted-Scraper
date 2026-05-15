package io.github.khanr1.tedscraper.r209

import io.github.khanr1.tedscraper.common.RichText
import io.github.khanr1.tedscraper.common.types.{
  OfficialName, StreetAddress, TownName, PostalCode,
  CountryCode, NutsCode, EmailAddress, PhoneNumber, FaxNumber, Url
}

// Mirrors ADDRESS_CONTRACTING_BODY, ADDRESS_CONTRACTOR, ADDRESS_REVIEW_BODY,
// and ADDRESS_CONTRACTING_BODY_ADDITIONAL in common_2014.xsd.
case class AddressContractingBody(
  officialName: Option[OfficialName],
  nationalId: Option[RichText],           // NATIONALID — new in R2.0.9
  address: Option[StreetAddress],
  town: Option[TownName],
  postalCode: Option[PostalCode],
  country: Option[CountryCode],
  nuts: List[NutsCode],                   // NUTS @CODE (namespace-agnostic)
  contactPoint: Option[RichText],
  phone: Option[PhoneNumber],
  fax: Option[FaxNumber],
  email: Option[EmailAddress],
  urlGeneral: Option[Url],                // URL_GENERAL
  urlBuyer: Option[Url],                  // URL_BUYER
  url: Option[Url]                        // URL (used in ADDRESS_CONTRACTOR)
)

object AddressContractingBody:
  val empty: AddressContractingBody = AddressContractingBody(
    None, None, None, None, None, None, Nil, None, None, None, None, None, None, None
  )
