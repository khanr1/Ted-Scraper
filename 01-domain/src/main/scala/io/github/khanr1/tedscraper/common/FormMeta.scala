package io.github.khanr1.tedscraper.common
import types.*

/** Attributes on every form element (CONTRACT_AWARD, CONTRACT etc.).
  * @param language
  *   \@LG — required, Language enum
  * @param category
  *   \@CATEGORY — ORIGINAL or TRANSLATION
  * @param version
  *   \@VERSION string
  * @param contractType
  *   \@CTYPE — Works | Services | Supplies
  */
case class FormMeta(
    language: Language,
    category: FormCategory,
    version: Option[RichText],
    contractType: Option[ContractType]
)
