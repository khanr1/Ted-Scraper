package io.github.khanr1.tedscraper.r208

import types.*

/** OJ number: validated issue number + optional CLASS attribute + LAST flag.
  * COLL_OJ is always "S" — modelled as a unit, not a String.
  */
case class OjNumber(number: OjIssueNumber, ojClass: OjClass, isLast: Boolean)

/** OJS reference — always collection "S" (Supplement). */
case class RefOjs(
    number: OjNumber,
    publicationDate: TedDate
)
