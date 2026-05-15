package io.github.khanr1.tedscraper.r209

import io.github.khanr1.tedscraper.common.RichText
import io.github.khanr1.tedscraper.common.types.{CpvCode, MonetaryAmount, Currency}

// Mirrors OBJECT_CONTRACT element (object_contract_fXX in XSD) — Section II.
case class ObjectContract(
  title: RichText,                             // TITLE > P
  referenceNumber: Option[RichText],           // REFERENCE_NUMBER
  cpvMain: CpvCode,                            // CPV_MAIN > CPV_CODE @CODE
  contractType: Option[String],                // TYPE_CONTRACT @CTYPE (SUPPLIES/SERVICES/WORKS)
  shortDescr: Option[RichText],                // SHORT_DESCR > P
  valEstimatedTotal: Option[MonetaryAmount],   // VAL_ESTIMATED_TOTAL text content
  valEstimatedCurrency: Option[Currency],      // VAL_ESTIMATED_TOTAL @CURRENCY
  valTotal: Option[MonetaryAmount],            // VAL_TOTAL (contract-level summary in F03)
  valTotalCurrency: Option[Currency],          // VAL_TOTAL @CURRENCY
  lotDivision: Boolean,                        // true=LOT_DIVISION, false=NO_LOT_DIVISION
  lots: List[ObjectDescr]                      // OBJECT_DESCR elements
)
