package io.github.khanr1.tedscraper.r208

import types.*

/** NOTICE_DATA (t_notice_data — common_prod.xsd).
  */
case class NoticeData(
    noDocOjs: Option[NoticeNumber],
    uriList: Option[List[UriDoc]],
    originalLanguage: Language,
    isoCountry: CountryCode,
    iaUrlGeneral: IaUrl,
    iaUrlEtendering: Option[IaUrl],
    originalCpvCodes: List[CpvEntry],
    currentCpvCodes: List[CpvEntry],
    originalNutsCodes: List[NutsEntry],
    currentNutsCodes: List[NutsEntry],
    valuesList: Option[List[ValuesEntry]],
    refNotice: Option[RefNotice]
)

case class UriDoc(language: Language, uri: IaUrl)
 
case class NoticeValue(
  amount:   MonetaryAmount,
  currency: Option[Currency],
  format:   Option[String]          // "NOT_STANDARD" or absent
)
 
sealed trait ValuesEntry
case class SingleValueEntry(valueType: ValuesType, value: NoticeValue) extends ValuesEntry
case class RangeValueEntry(valueType: ValuesType, values: List[NoticeValue]) extends ValuesEntry
 
case class RefNotice(noticeNumbers: List[NoticeNumber])