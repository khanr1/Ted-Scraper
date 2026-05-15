package io.github.khanr1.tedscraper.r209

import io.github.khanr1.tedscraper.common.RichText

// Mirrors LEFTI element (lefti_fXX in XSD) — Section III.
// Present in F01, F02, F04, F05, F12, F21. Absent in F03, F08, F14, F15.
case class Lefti(
  suitability: Option[RichText],               // SUITABILITY
  economicCriteriaDoc: Option[Boolean],        // ECONOMIC_CRITERIA_DOC presence
  economicFinancialInfo: Option[RichText],     // ECONOMIC_FINANCIAL_INFO
  economicMinLevel: Option[RichText],          // ECONOMIC_FINANCIAL_MIN_LEVEL
  technicalCriteriaDoc: Option[Boolean],       // TECHNICAL_CRITERIA_DOC presence
  technicalProfessionalInfo: Option[RichText], // TECHNICAL_PROFESSIONAL_INFO
  technicalMinLevel: Option[RichText],         // TECHNICAL_MINIMUM_LEVEL
  reservedSheltered: Option[Boolean],          // RESTRICTED_SHELTERED_WORKSHOP
  performanceConditions: Option[RichText],     // PERFORMANCE_CONDITIONS
  staffQualification: Option[Boolean]          // PERFORMANCE_STAFF_QUALIFICATION
)
