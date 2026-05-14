package io.github.khanr1.tedscraper.r209

import io.github.khanr1.tedscraper.r208.RichText
import io.github.khanr1.tedscraper.r208.types.{MonetaryAmount, Currency}

// A single contractor from CONTRACTOR > ADDRESS_CONTRACTOR + SME/NO_SME.
case class Contractor(
  address: AddressContractingBody,
  isSme: Option[Boolean]              // Some(true)=SME, Some(false)=NO_SME, None=absent
)

// Mirrors AWARDED_CONTRACT child of AWARD_CONTRACT when no NO_AWARDED_CONTRACT.
case class AwardedContract(
  dateConclusion: Option[String],     // DATE_CONCLUSION_CONTRACT ISO "YYYY-MM-DD"
  tendersReceived: Option[Int],       // NB_TENDERS_RECEIVED
  tendersReceivedSme: Option[Int],    // NB_TENDERS_RECEIVED_SME
  tendersReceivedOtherEu: Option[Int],// NB_TENDERS_RECEIVED_OTHER_EU
  tendersReceivedNonEu: Option[Int],  // NB_TENDERS_RECEIVED_NON_EU
  awardedToGroup: Boolean,            // AWARDED_TO_GROUP presence
  contractors: List[Contractor],      // CONTRACTOR elements
  valEstimated: Option[MonetaryAmount],     // VALUES > VAL_ESTIMATED_TOTAL
  valEstimatedCurrency: Option[Currency],
  valTotal: Option[MonetaryAmount],         // VALUES > VAL_TOTAL text
  valTotalCurrency: Option[Currency],       // VALUES > VAL_TOTAL @CURRENCY
  valRangeLow: Option[MonetaryAmount],      // VALUES > VAL_RANGE_TOTAL @LOW
  valRangeHigh: Option[MonetaryAmount],     // VALUES > VAL_RANGE_TOTAL @HIGH
  valRangeCurrency: Option[Currency],       // VALUES > VAL_RANGE_TOTAL @CURRENCY
  subcontracted: Option[Boolean]            // LIKELY_SUBCONTRACTED presence
)

// Mirrors AWARD_CONTRACT ITEM="N" element — Section V.
case class AwardContractItem(
  item: Int,                          // ITEM attribute
  contractNo: Option[RichText],       // CONTRACT_NO
  lotNo: Option[RichText],            // LOT_NO
  title: Option[RichText],            // TITLE > P
  noAward: Boolean,                   // true = NO_AWARDED_CONTRACT
  awarded: Option[AwardedContract]    // present when noAward = false
)
