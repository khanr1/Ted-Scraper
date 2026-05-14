package io.github.khanr1.tedscraper.r208

package types

// CODE attribute on AA_AUTHORITY_TYPE.
  enum AuthorityTypeCode:
    case `1`  // Ministry or any other national or federal authority
    case `2`  // National or federal agency/office
    case `3`  // Regional or local authority
    case `4`  // Regional or local agency/office
    case `5`  // Body governed by public law
    case `6`  // European institution/agency or international organisation
    case `8`  // Other
    case `9`  // Not specified
    case N    // National agency
    case R    // Regional authority
    case Z    // Not specified (alternative)
    case Unknown(raw: String)

object AuthorityTypeCode:
    private val mapping: Map[String, AuthorityTypeCode] = Map("Ministry or any other national or federal authority" -> `1`, "National or federal agency/office" -> `2`, "Regional or local authority" -> `3`, "Regional or local agency/office" -> `4`, "Body governed by public law" -> `5`, "European institution/agency or international organisation" -> `6`, "Other" -> `8`, "Not specified" -> `9`, "National agency" -> N, "Regional authority" -> R, "Not specified (alternative)" -> Z)
    private val reverseMapping: Map[AuthorityTypeCode, String] = mapping.map(_.swap)
    def from(s: String): AuthorityTypeCode = mapping.getOrElse(s, Unknown(s))