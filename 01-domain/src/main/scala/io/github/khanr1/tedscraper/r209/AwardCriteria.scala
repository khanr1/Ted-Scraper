package io.github.khanr1.tedscraper.r209

import io.github.khanr1.tedscraper.r208.RichText

// Mirrors the AC element and award_criteria / award_criteria_doc groups
// in common_2014.xsd: AC_PRICE (empty elem), AC_QUALITY array, AC_COST array.
case class AwardCriterion(name: RichText, weight: Option[RichText])

case class AwardCriteria(
  lowestPrice: Boolean,                  // AC_PRICE presence
  qualityCriteria: List[AwardCriterion], // AC_QUALITY elements
  costCriteria: List[AwardCriterion]     // AC_COST elements
)
