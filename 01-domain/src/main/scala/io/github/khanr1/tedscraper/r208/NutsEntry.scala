package io.github.khanr1.tedscraper.r208

import types.*

/** NUTS code with its label (t_nuts_code in common_prod.xsd). Covers both NUTS
  * (S02) and n2016:NUTS (S03+) — same opaque type.
  */
case class NutsEntry(code: NutsCode, label: RichText)
