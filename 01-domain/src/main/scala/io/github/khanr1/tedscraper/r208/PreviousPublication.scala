package io.github.khanr1.tedscraper.r208

import types.*

case class PreviousPublication(
    noticeNumber: NoticeNumber,
    date: Option[StructuredDate],
    noticeType: Option[PrevPublicationType]
)

sealed trait PrevPublicationType
case object ContractNoticeRef          extends PrevPublicationType
case object PriorInfoNoticeRef         extends PrevPublicationType
case object PrevNoticeBuyerProfileRef  extends PrevPublicationType