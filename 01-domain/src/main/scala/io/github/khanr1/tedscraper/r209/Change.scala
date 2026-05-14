package io.github.khanr1.tedscraper.r209

// Mirrors a CHANGE element inside CHANGES in F14_2014 (Corrigendum).
case class Change(
  section: Option[String],   // WHERE > SECTION
  lotNo: Option[String],     // WHERE > LOT_NO
  label: Option[String],     // WHERE > LABEL
  oldValue: Option[String],  // OLD_VALUE text (data group serialised)
  newValue: Option[String]   // NEW_VALUE text (data group serialised)
)
