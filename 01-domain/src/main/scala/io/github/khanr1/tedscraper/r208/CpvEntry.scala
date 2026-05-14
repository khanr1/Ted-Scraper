package io.github.khanr1.tedscraper.r208

import types.*

/** CPV code with its label (t_cpv_code in common_prod.xsd).
  */
case class CpvEntry(code: CpvCode, label: RichText)
