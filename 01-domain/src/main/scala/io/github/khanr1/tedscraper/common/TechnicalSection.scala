package io.github.khanr1.tedscraper.common

import types.*

/** @param receptionId
  *   Validated pattern \d{2}-\d{6}-\d{3}
  * @param deletionDate
  *   yyyymmdd
  * @param formLanguages
  *   Space-separated from FORM_LG_LIST, split into typed list
  * @param comments
  *   Optional rich text
  * @param oldHeading
  *   Optional legacy heading string
  */
case class TechnicalSection(
    receptionId: ReceptionId,
    deletionDate: TedDate,
    formLanguages: List[Language],
    comments: Option[RichText],
    oldHeading: Option[HeadingCode]
)
