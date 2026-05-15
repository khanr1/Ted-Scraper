package io.github.khanr1.tedscraper.r208

import io.github.khanr1.tedscraper.common.RichText

sealed trait ProcedureType
case object OpenProcedure                extends ProcedureType
case object RestrictedProcedure          extends ProcedureType
case object AcceleratedRestricted        extends ProcedureType
case object CompetitiveDialogue          extends ProcedureType
case object NegotiatedWithCompetition    extends ProcedureType
case object AcceleratedNegotiated        extends ProcedureType
case class  NegotiatedWithoutCompetition(annexD: Option[RichText]) extends ProcedureType
case class  AwardWithoutPriorPublication(annexD: Option[RichText]) extends ProcedureType
