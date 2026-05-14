package io.github.khanr1.tedscraper.r208

case class ComplementaryInfoAward(
    relatesToEuProject: Option[Boolean],
    additionalInfo: Option[RichText],
    proceduresForAppeal: Option[ProceduresForAppeal],
    dispatchDate: Option[StructuredDate]
)

case class ComplementaryInfoNotice(
    relatesToEuProject: Option[Boolean],
    additionalInfo: Option[RichText],
    proceduresForAppeal: Option[ProceduresForAppeal],
    dispatchDate: Option[StructuredDate]
)
