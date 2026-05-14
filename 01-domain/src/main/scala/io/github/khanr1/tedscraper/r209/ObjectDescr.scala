package io.github.khanr1.tedscraper.r209

import io.github.khanr1.tedscraper.r208.RichText
import io.github.khanr1.tedscraper.r208.types.{CpvCode, NutsCode, MonetaryAmount, Currency}

// Mirrors OBJECT_DESCR ITEM="N" element (object_fXX types in XSD) — one per lot.
case class ObjectDescr(
  item: Int,                             // ITEM attribute
  title: Option[RichText],               // TITLE > P
  lotNo: Option[RichText],               // LOT_NO
  cpvAdditional: List[CpvCode],          // CPV_ADDITIONAL > CPV_CODE @CODE
  nuts: List[NutsCode],                  // NUTS @CODE (any namespace variant)
  mainSite: Option[RichText],            // MAIN_SITE > P
  shortDescr: Option[RichText],          // SHORT_DESCR > P
  awardCriteria: Option[AwardCriteria],  // AC element
  valObject: Option[MonetaryAmount],     // VAL_OBJECT text content
  valObjectCurrency: Option[Currency],   // VAL_OBJECT @CURRENCY
  durationType: Option[String],          // DURATION @TYPE (MONTH/DAY)
  durationValue: Option[Int],            // DURATION text content
  dateStart: Option[String],             // DATE_START ISO
  dateEnd: Option[String],               // DATE_END ISO
  renewal: Option[Boolean],              // RENEWAL/NO_RENEWAL
  renewalDescr: Option[RichText],        // RENEWAL_DESCR
  acceptedVariants: Option[Boolean],     // ACCEPTED_VARIANTS/NO_ACCEPTED_VARIANTS
  options: Option[Boolean],              // OPTIONS/NO_OPTIONS
  optionsDescr: Option[RichText],        // OPTIONS_DESCR
  euProject: Option[RichText],           // EU_PROGR_RELATED text
  infoAdd: Option[RichText]              // INFO_ADD
)
