package io.github.khanr1.tedscraper.r209

import io.github.khanr1.tedscraper.common.RichText
import io.github.khanr1.tedscraper.common.types.{OfficialName, CountryCode}

// Mirrors COMPLEMENTARY_INFO element (ci_fXX in XSD) — Section VI.
case class ComplementaryInfo(
  recurrentProcurement: Option[Boolean], // RECURRENT_PROCUREMENT / NO_RECURRENT_PROCUREMENT
  estimatedTiming: Option[RichText],     // ESTIMATED_TIMING
  eOrdering: Boolean,                    // EORDERING presence
  eInvoicing: Boolean,                   // EINVOICING presence
  ePayment: Boolean,                     // EPAYMENT presence
  infoAdd: Option[RichText],             // INFO_ADD > P
  reviewBodyName: Option[OfficialName],  // ADDRESS_REVIEW_BODY > OFFICIALNAME
  reviewBodyCountry: Option[CountryCode],// ADDRESS_REVIEW_BODY > COUNTRY @VALUE
  dateDispatch: Option[String]           // DATE_DISPATCH_NOTICE ISO "YYYY-MM-DD"
)
