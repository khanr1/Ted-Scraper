package io.github.khanr1.tedscraper
package services.r208

import cats.effect.Async
import fs2.Stream

import r208.*
import r208.types.*

trait r208Services[F[_]]:
  def toCSV(notices: Stream[F, r208.Notice]): Stream[F, String]

object r208Services:

  def make[F[_]: Async](noticeRepo: repositories.r208.NoticeRepository[F]): r208Services[F] =
    new r208Services[F]:
      def toCSV(notices: Stream[F, r208.Notice]): Stream[F, String] =
        Stream.emit(csvHeader) ++ notices.flatMap(n => Stream.emits(noticeToRows(n)))

  // ── CSV infrastructure ──────────────────────────────────────────────────────

  private val csvHeader: String = List(
    "schema_version",
    "notice_oj_id", "notice_type", "publication_date", "dispatch_date",
    "ca_name", "ca_town", "ca_country", "authority_type",
    "contract_type", "contract_title", "short_description",
    "cpv_code", "cpv_additional", "nuts_code",
    "procedure_type", "award_criteria_summary", "procedure_justification",
    "lot_number", "award_contract_number", "award_contract_title",
    "lot_description", "lot_cpv",
    "award_date", "total_final_value", "total_final_currency",
    "estimated_value", "estimated_currency",
    "offers_received",
    "winner_name", "winner_town", "winner_country",
    "ref_notice_number", "relates_to_eu_project"
  ).mkString(",")

  private def escape(s: String): String =
    if s.exists(c => c == ',' || c == '"' || c == '\n' || c == '\r') then
      "\"" + s.replace("\"", "\"\"") + "\""
    else s

  // 35 fields — productIterator renders in declaration order, must match csvHeader
  private case class Row(
    schemaVersion: String = "",
    noticeOjId: String = "", noticeType: String = "",
    publicationDate: String = "", dispatchDate: String = "",
    caName: String = "", caTown: String = "", caCountry: String = "",
    authorityType: String = "",
    contractType: String = "", contractTitle: String = "",
    shortDescription: String = "",
    cpvCode: String = "", cpvAdditional: String = "", nutsCode: String = "",
    procedureType: String = "", awardCriteriaSummary: String = "",
    procJustification: String = "",
    lotNumber: String = "", awardContractNumber: String = "",
    awardContractTitle: String = "",
    lotDescription: String = "", lotCpv: String = "",
    awardDate: String = "",
    totalFinalValue: String = "", totalFinalCurrency: String = "",
    estimatedValue: String = "", estimatedCurrency: String = "",
    offersReceived: String = "",
    winnerName: String = "", winnerTown: String = "", winnerCountry: String = "",
    refNoticeNumber: String = "", relatesToEuProject: String = ""
  ):
    def toCsvLine: String =
      productIterator.map(_.asInstanceOf[String]).map(escape).mkString(",")

  // ── Field extractors ────────────────────────────────────────────────────────

  private def noticeTypeLabel(code: String): String = code match
    case "F01"     => "Prior Information Notice"
    case "F02"     => "Contract Notice"
    case "F03"     => "Contract Award Notice"
    case "F04"     => "Periodic Indicative Notice (Utilities)"
    case "F05"     => "Contract Notice (Utilities)"
    case "F06"     => "Contract Award Notice (Utilities)"
    case "F07"     => "Qualification System (Utilities)"
    case "F08"     => "Buyer Profile"
    case "F09"     => "Simplified Contract Notice"
    case "F10"     => "Concession Notice"
    case "F11"     => "Contract Award (Concession)"
    case "F12"     => "Design Contest Notice"
    case "F13"     => "Design Contest Result"
    case "F14"     => "Corrigendum"
    case "F15"     => "Voluntary Ex Ante Transparency Notice"
    case "F16"     => "Prior Information (Defence)"
    case "F17"     => "Contract Notice (Defence)"
    case "F18"     => "Contract Award (Defence)"
    case "F19"     => "Subcontract Notice (Defence)"
    case "T01"     => "Prior Information (Transport)"
    case "T02"     => "Contract Award (Transport)"
    case "OTH_NOT" => "Other Notice"
    case "EEIG"    => "EEIG Notice"
    case other     => other

  private def valueAndCurrency(fcv: Option[FormContractValue]): (String, String) =
    fcv match
      case None                                          => ("", "")
      case Some(ExactValue(ValueCost(amt, cur)))         => (amt.value.toString, cur.toString)
      case Some(RangeValue(RangeValueCost(lo, hi, cur))) => (s"${lo.value}-${hi.value}", cur.toString)

  private def totalFinalFields(tfv: Option[TotalFinalValue]): (String, String) =
    tfv.fold(("", ""))(t => valueAndCurrency(Some(t.value)))

  private def cpvFields(cpv: Option[CpvCodes]): (String, String) =
    cpv.fold(("", ""))(c => (c.main.value, c.additional.map(_.value).mkString(";")))

  private def nutsStr(loc: Option[LocationNuts]): String =
    loc.fold("")(_.nutsCodes.map(_.value).mkString(";"))

  private def awardDateStr(d: Option[StructuredDate]): String =
    d.fold("")(s => s"${s.year.value}-${s.month.value}-${s.day.value}")

  private def winnerFields(cd: Option[ContactData]): (String, String, String) =
    cd.fold(("", "", "")) { c =>
      (c.officialName.fold("")(_.value), c.town.fold("")(_.value), c.country.fold("")(_.toString))
    }

  private def criteriaStr(codifLabel: String, detail: Option[AwardCriteriaDetail] = None): String =
    detail match
      case Some(d) if d.lowestPrice       => "Lowest price"
      case Some(d) if d.criteria.nonEmpty =>
        "MEAT: " + d.criteria.map(c =>
          s"${c.name.value}${c.weighting.fold("")(w => s" ${w.value}%")}").mkString("; ")
      case _ => codifLabel

  private def procedureTypeStr(pt: Option[ProcedureType]): String = pt match
    case None                                  => ""
    case Some(OpenProcedure)                   => "Open"
    case Some(RestrictedProcedure)             => "Restricted"
    case Some(AcceleratedRestricted)           => "Accelerated Restricted"
    case Some(CompetitiveDialogue)             => "Competitive Dialogue"
    case Some(NegotiatedWithCompetition)       => "Negotiated with Competition"
    case Some(AcceleratedNegotiated)           => "Accelerated Negotiated"
    case Some(NegotiatedWithoutCompetition(_)) => "Negotiated without Competition"
    case Some(AwardWithoutPriorPublication(_)) => "Direct Award"

  private def annexDStr(pt: Option[ProcedureType]): String = pt match
    case Some(NegotiatedWithoutCompetition(annexD)) => annexD.fold("")(_.value)
    case Some(AwardWithoutPriorPublication(annexD)) => annexD.fold("")(_.value)
    case _                                          => ""

  // ── Base row: envelope + CA ─────────────────────────────────────────────────

  private def baseRow(
    n: Notice,
    formType: String,
    ca: ContractingProfile,
    pob: Option[PurchasingOnBehalf] = None,
    criteriaDetail: Option[AwardCriteriaDetail] = None
  ): Row =
    val cd = n.codedDataSection.codifData
    val nd = n.codedDataSection.noticeData
    val (effectiveName, effectiveTown, effectiveCountry) = pob match
      case Some(PurchasingYes(c :: _)) =>
        (
          c.officialName.fold(ca.officialName.value)(_.value),
          c.town.fold(ca.town.value)(_.value),
          c.country.fold(ca.country.toString)(_.toString)
        )
      case _ =>
        (ca.officialName.value, ca.town.value, ca.country.toString)
    Row(
      schemaVersion     = n.schemaVersion.value,
      noticeOjId        = nd.noDocOjs.fold("")(_.value),
      noticeType        = noticeTypeLabel(formType),
      publicationDate   = n.codedDataSection.refOjs.publicationDate.toIso,
      dispatchDate      = cd.dispatchDate.toIso,
      caName            = effectiveName,
      caTown            = effectiveTown,
      caCountry         = effectiveCountry,
      authorityType     = cd.authorityType.label.value,
      awardCriteriaSummary = criteriaStr(cd.awardCriteria.label.value, criteriaDetail),
      refNoticeNumber   = nd.refNotice.fold("")(_.noticeNumbers.map(_.value).mkString(";"))
    )

  // ── Award helpers ───────────────────────────────────────────────────────────

  private def withAward(row: Row, a: AwardOfContract): Row =
    val (finalVal, finalCur) = a.contractValue.fold(("", ""))(cv => valueAndCurrency(cv.finalValue))
    val (wName, wTown, wCty) = winnerFields(a.winner)
    row.copy(
      lotNumber           = a.lotNumbers.map(_.value).mkString(";"),
      awardDate           = awardDateStr(a.awardDate),
      awardContractNumber = a.contractNumber.fold("")(_.value),
      awardContractTitle  = a.contractTitle.fold("")(_.value),
      totalFinalValue     = finalVal,
      totalFinalCurrency  = finalCur,
      offersReceived      = a.offersReceived.fold("")(_.toString),
      winnerName          = wName,
      winnerTown          = wTown,
      winnerCountry       = wCty
    )

  private def withF06Award(row: Row, a: AwardAndContractValue): Row =
    val (finalVal, finalCur) = a.pricePaid match
      case Some(vc) => (vc.amount.value.toString, vc.currency.toString)
      case None     => a.valueInfo.fold(("", ""))(vi => valueAndCurrency(vi.finalValue))
    val (wName, wTown, wCty) = winnerFields(a.winner)
    row.copy(
      lotNumber           = a.lotNumbers.map(_.value).mkString(";"),
      awardDate           = awardDateStr(a.awardDate),
      awardContractNumber = a.contractNo.fold("")(_.value),
      awardContractTitle  = a.contractTitle.fold("")(_.value),
      totalFinalValue     = finalVal,
      totalFinalCurrency  = finalCur,
      offersReceived      = a.offersReceived.fold("")(_.toString),
      winnerName          = wName,
      winnerTown          = wTown,
      winnerCountry       = wCty
    )

  private def expandAwards(base: Row, awards: List[AwardOfContract]): List[String] =
    if awards.isEmpty then List(base.toCsvLine)
    else awards.map(a => withAward(base, a).toCsvLine)

  // ── Top-level dispatch ──────────────────────────────────────────────────────

  private def noticeToRows(n: Notice): List[String] =
    n.formSection.forms.flatMap(formToRows(n, _))

  private def formToRows(n: Notice, fb: FormBody): List[String] = fb match
    case PriorInformationForm(d)                 => f01Rows(n, d)
    case ContractNoticeForm(d)                   => f02Rows(n, d)
    case ContractAwardForm(d)                    => f03Rows(n, d)
    case PeriodicIndicativeUtilitiesForm(d)      => f04Rows(n, d)
    case ContractUtilitiesForm(d)                => f05Rows(n, d)
    case ContractAwardUtilitiesForm(d)           => f06Rows(n, d)
    case QualificationSystemUtilitiesForm(d)     => List(f07Row(n, d))
    case BuyerProfileForm(d)                     => List(f08Row(n, d))
    case SimplifiedContractForm(d)               => List(f09Row(n, d))
    case ConcessionForm(d)                       => List(f10Row(n, d))
    case ContractConcessionaireForm(d)           => List(f11Row(n, d))
    case DesignContestForm(d)                    => List(f12Row(n, d))
    case ResultDesignContestForm(d)              => List(f13Row(n, d))
    case AdditionalInformationCorrigendumForm(d) => List(f14Row(n, d))
    case VeatForm(d)                             => f15Rows(n, d)
    case PriorInformationDefenceForm(d)          => f16Rows(n, d)
    case ContractDefenceForm(d)                  => List(f17Row(n, d))
    case ContractAwardDefenceForm(d)             => f18Rows(n, d)
    case ContractSubDefenceForm(d)               => List(f19Row(n, d))
    case PriorInformationMoveForm(d)             => t01Rows(n, d)
    case ContractMoveForm(d)                     => t02Rows(n, d)
    case OtherNoticeForm(d)                      => List(othNotRow(n, d))
    case EeigForm(d)                             => List(eeigRow(n, d))

  // ── F01 ─────────────────────────────────────────────────────────────────────

  private def f01Rows(n: Notice, d: F01PriorInformation): List[String] =
    val obj          = d.contractObject
    val (estV, estC) = valueAndCurrency(obj.estimatedValue)
    val (cpvM, cpvA) = cpvFields(obj.cpv)
    val base = baseRow(n, "F01", d.contractingAuthority, d.purchasingOnBehalf)
      .copy(
        contractType       = obj.contractType.fold("")(_.toString),
        cpvCode            = cpvM, cpvAdditional = cpvA,
        estimatedValue     = estV, estimatedCurrency = estC,
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      )
    if obj.lots.isEmpty then List(base.toCsvLine)
    else obj.lots.map { lot =>
      val (lCpv, _) = cpvFields(lot.cpv)
      base.copy(
        lotNumber          = lot.lotNumber.fold("")(_.value),
        awardContractTitle = lot.lotTitle.fold("")(_.value),
        lotDescription     = lot.lotDescription.fold("")(_.value),
        lotCpv             = lCpv
      ).toCsvLine
    }

  // ── F02 ─────────────────────────────────────────────────────────────────────

  private def f02Rows(n: Notice, d: F02ContractNotice): List[String] =
    val obj          = d.contractObject
    val (estV, estC) = valueAndCurrency(obj.estimatedValue)
    val (cpvM, cpvA) = cpvFields(obj.cpv)
    val base = baseRow(n, "F02", d.contractingAuthority, d.purchasingOnBehalf, d.procedure.awardCriteria)
      .copy(
        contractType       = obj.contractType.fold("")(_.toString),
        contractTitle      = obj.title.fold("")(_.value),
        shortDescription   = obj.description.value,
        cpvCode            = cpvM, cpvAdditional = cpvA,
        nutsCode           = nutsStr(obj.location),
        procedureType      = procedureTypeStr(d.procedure.procedureType),
        procJustification  = annexDStr(d.procedure.procedureType),
        estimatedValue     = estV, estimatedCurrency = estC,
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      )
    if obj.lots.isEmpty then List(base.toCsvLine)
    else obj.lots.map { lot =>
      val (lCpv, _)      = cpvFields(lot.cpv)
      val (lEstV, lEstC) = valueAndCurrency(lot.estimatedValue)
      base.copy(
        lotNumber          = lot.lotNumber.fold("")(_.value),
        awardContractTitle = lot.lotTitle.fold("")(_.value),
        lotDescription     = lot.description.fold("")(_.value),
        lotCpv             = lCpv,
        estimatedValue     = if lEstV.nonEmpty then lEstV else estV,
        estimatedCurrency  = if lEstC.nonEmpty then lEstC else estC
      ).toCsvLine
    }

  // ── F03 ─────────────────────────────────────────────────────────────────────

  private def f03Rows(n: Notice, d: F03ContractAward): List[String] =
    val obj          = d.contractObject
    val (tfV, tfC)   = totalFinalFields(obj.totalFinalValue)
    val (cpvM, cpvA) = cpvFields(obj.cpv)
    val base = baseRow(n, "F03", d.contractingAuthority, d.purchasingOnBehalf)
      .copy(
        contractType       = obj.contractType.fold("")(_.toString),
        contractTitle      = obj.title.fold("")(_.value),
        shortDescription   = obj.description.value,
        cpvCode            = cpvM, cpvAdditional = cpvA,
        nutsCode           = nutsStr(obj.location),
        procedureType      = procedureTypeStr(d.procedure.procedureType),
        procJustification  = annexDStr(d.procedure.procedureType),
        totalFinalValue    = tfV, totalFinalCurrency = tfC,
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      )
    expandAwards(base, d.awards)

  // ── F04 ─────────────────────────────────────────────────────────────────────

  private def f04Rows(n: Notice, d: F04PeriodicIndicativeUtilities): List[String] =
    val criteriaDetail = d.procedureAdmin.headOption.flatMap(_.awardCriteria)
    val base = baseRow(n, "F04", d.contractingEntity, None, criteriaDetail)
      .copy(shortDescription = d.activitiesOfEntity.fold("")(_.value))
    val rows = d.contractObjects.flatMap { co =>
      val (estV, estC) = valueAndCurrency(co.estimatedValue)
      val (cpvM, cpvA) = cpvFields(co.cpv)
      val coBase = base.copy(
        shortDescription  = co.description.fold("")(_.value),
        cpvCode           = cpvM, cpvAdditional = cpvA,
        nutsCode          = nutsStr(co.location),
        estimatedValue    = estV, estimatedCurrency = estC
      )
      if co.lots.isEmpty then List(coBase)
      else co.lots.map { lot =>
        val (lEstV, lEstC) = valueAndCurrency(lot.estimatedValue)
        coBase.copy(
          lotNumber         = lot.lotNumber.fold("")(_.value),
          lotDescription    = lot.description.fold("")(_.value),
          estimatedValue    = if lEstV.nonEmpty then lEstV else estV,
          estimatedCurrency = if lEstC.nonEmpty then lEstC else estC
        )
      }
    }
    if rows.isEmpty then List(base.toCsvLine) else rows.map(_.toCsvLine)

  // ── F05 ─────────────────────────────────────────────────────────────────────

  private def f05Rows(n: Notice, d: F05ContractUtilities): List[String] =
    val obj          = d.contractObject
    val (estV, estC) = valueAndCurrency(obj.estimatedValue)
    val (cpvM, cpvA) = cpvFields(obj.cpv)
    val base = baseRow(n, "F05", d.contractingEntity, None, d.procedure.awardCriteria)
      .copy(
        contractType       = obj.contractType.fold("")(_.toString),
        contractTitle      = obj.title.fold("")(_.value),
        shortDescription   = obj.description.value,
        cpvCode            = cpvM, cpvAdditional = cpvA,
        nutsCode           = nutsStr(obj.location),
        procedureType      = d.procedure.procedureType.fold("")(_.value),
        estimatedValue     = estV, estimatedCurrency = estC,
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      )
    if obj.lots.isEmpty then List(base.toCsvLine)
    else obj.lots.map { lot =>
      val (lCpv, _) = cpvFields(lot.cpv)
      base.copy(
        lotNumber      = lot.lotNumber.fold("")(_.value),
        lotDescription = lot.description.fold("")(_.value),
        lotCpv         = lCpv
      ).toCsvLine
    }

  // ── F06 ─────────────────────────────────────────────────────────────────────

  private def f06Rows(n: Notice, d: F06ContractAwardUtilities): List[String] =
    val obj          = d.contractObject
    val (tfV, tfC)   = totalFinalFields(obj.totalFinalValue)
    val (cpvM, cpvA) = cpvFields(obj.cpv)
    val base = baseRow(n, "F06", d.contractingEntity, None, d.procedures.awardCriteria)
      .copy(
        contractType       = obj.contractType.fold("")(_.toString),
        contractTitle      = obj.title.fold("")(_.value),
        shortDescription   = obj.description.fold("")(_.value),
        cpvCode            = cpvM, cpvAdditional = cpvA,
        nutsCode           = nutsStr(obj.location),
        procedureType      = procedureTypeStr(d.procedures.procedureType),
        procJustification  = annexDStr(d.procedures.procedureType),
        totalFinalValue    = tfV, totalFinalCurrency = tfC,
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      )
    if d.awards.isEmpty then List(base.toCsvLine)
    else d.awards.map(a => withF06Award(base, a).toCsvLine)

  // ── F07 ─────────────────────────────────────────────────────────────────────

  private def f07Row(n: Notice, d: F07QualificationSystemUtilities): String =
    baseRow(n, "F07", d.contractingEntity)
      .copy(
        shortDescription   = d.activitiesOfEntity.fold("")(_.value),
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      ).toCsvLine

  // ── F08 ─────────────────────────────────────────────────────────────────────

  private def f08Row(n: Notice, d: F08BuyerProfile): String =
    baseRow(n, "F08", d.contractingAuthority).toCsvLine

  // ── F09 ─────────────────────────────────────────────────────────────────────

  private def f09Row(n: Notice, d: F09SimplifiedContract): String =
    val obj          = d.contractObject
    val (cpvM, cpvA) = cpvFields(obj.cpv)
    baseRow(n, "F09", d.contractingAuthority)
      .copy(
        contractType       = obj.contractType.fold("")(_.toString),
        contractTitle      = obj.title.fold("")(_.value),
        shortDescription   = obj.description.value,
        cpvCode            = cpvM, cpvAdditional = cpvA,
        nutsCode           = nutsStr(obj.location),
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      ).toCsvLine

  // ── F10 ─────────────────────────────────────────────────────────────────────

  private def f10Row(n: Notice, d: F10Concession): String =
    val obj      = d.contractObject
    val (cpvM, _) = cpvFields(obj.cpv)
    baseRow(n, "F10", d.contractingAuthority)
      .copy(
        contractTitle      = obj.title.fold("")(_.value),
        shortDescription   = obj.description.value,
        cpvCode            = cpvM,
        nutsCode           = nutsStr(obj.location),
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      ).toCsvLine

  // ── F11 ─────────────────────────────────────────────────────────────────────

  private def f11Row(n: Notice, d: F11ContractConcessionaire): String =
    val obj          = d.contractObject
    val (cpvM, _)    = cpvFields(obj.cpv)
    val (estV, estC) = valueAndCurrency(obj.estimatedValue)
    baseRow(n, "F11", d.contractingConcessionaire)
      .copy(
        contractTitle      = obj.title.fold("")(_.value),
        shortDescription   = obj.description.value,
        cpvCode            = cpvM,
        nutsCode           = nutsStr(obj.location),
        estimatedValue     = estV, estimatedCurrency = estC,
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      ).toCsvLine

  // ── F12 ─────────────────────────────────────────────────────────────────────

  private def f12Row(n: Notice, d: F12DesignContest): String =
    val obj          = d.contestObject
    val (cpvM, _)    = cpvFields(obj.cpv)
    val (estV, estC) = valueAndCurrency(obj.estimatedValue)
    baseRow(n, "F12", d.contractingAuthority)
      .copy(
        contractTitle      = obj.title.fold("")(_.value),
        shortDescription   = obj.description.value,
        cpvCode            = cpvM,
        nutsCode           = nutsStr(obj.location),
        estimatedValue     = estV, estimatedCurrency = estC,
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      ).toCsvLine

  // ── F13 ─────────────────────────────────────────────────────────────────────

  private def f13Row(n: Notice, d: F13ResultDesignContest): String =
    val obj      = d.contestObject
    val (cpvM, _) = cpvFields(obj.cpv)
    val (pV, pC) = valueAndCurrency(d.results.prizeValue)
    baseRow(n, "F13", d.contractingEntity)
      .copy(
        contractTitle      = obj.title.fold("")(_.value),
        shortDescription   = obj.description.fold("")(_.value),
        cpvCode            = cpvM,
        totalFinalValue    = pV, totalFinalCurrency = pC,
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      ).toCsvLine

  // ── F14 ─────────────────────────────────────────────────────────────────────

  private def f14Row(n: Notice, d: F14AdditionalInformationCorrigendum): String =
    baseRow(n, "F14", d.contractingAuthority)
      .copy(
        shortDescription = d.complementaryInfo.additionalInfo.fold("")(_.value)
      ).toCsvLine

  // ── F15 ─────────────────────────────────────────────────────────────────────

  private def f15Rows(n: Notice, d: F15VeatNotice): List[String] =
    val obj          = d.contractObject
    val (tfV, tfC)   = totalFinalFields(obj.totalValue)
    val (cpvM, cpvA) = cpvFields(obj.cpv)
    val justif = d.procedure.justification.fold(annexDStr(d.procedure.procedureType))(_.value)
    val base = baseRow(n, "F15", d.contractingAuthority, d.purchasingOnBehalf)
      .copy(
        contractType       = obj.contractType.fold("")(_.toString),
        contractTitle      = obj.title.fold("")(_.value),
        shortDescription   = obj.description.value,
        cpvCode            = cpvM, cpvAdditional = cpvA,
        nutsCode           = nutsStr(obj.location),
        procedureType      = procedureTypeStr(d.procedure.procedureType),
        procJustification  = justif,
        totalFinalValue    = tfV, totalFinalCurrency = tfC,
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      )
    expandAwards(base, d.awards)

  // ── F16 ─────────────────────────────────────────────────────────────────────

  private def f16Rows(n: Notice, d: F16PriorInformationDefence): List[String] =
    val baseRow0 = baseRow(n, "F16", d.contractingAuthority)
      .copy(relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString))
    if d.contractObjects.isEmpty then List(baseRow0.toCsvLine)
    else d.contractObjects.map { co =>
      val (cpvM, _)    = cpvFields(co.cpv)
      val (estV, estC) = valueAndCurrency(co.estimatedValue)
      baseRow0.copy(
        contractType     = co.contractType.fold("")(_.toString),
        shortDescription = co.description.fold("")(_.value),
        cpvCode          = cpvM,
        nutsCode         = nutsStr(co.location),
        estimatedValue   = estV, estimatedCurrency = estC
      ).toCsvLine
    }

  // ── F17 ─────────────────────────────────────────────────────────────────────

  private def f17Row(n: Notice, d: F17ContractDefence): String =
    val obj          = d.contractObject
    val (cpvM, cpvA) = cpvFields(obj.cpv)
    baseRow(n, "F17", d.contractingAuthority)
      .copy(
        contractType       = obj.contractType.fold("")(_.toString),
        contractTitle      = obj.title.fold("")(_.value),
        shortDescription   = obj.description.value,
        cpvCode            = cpvM, cpvAdditional = cpvA,
        nutsCode           = nutsStr(obj.location),
        procedureType      = procedureTypeStr(d.procedure.procedureType),
        procJustification  = annexDStr(d.procedure.procedureType),
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      ).toCsvLine

  // ── F18 ─────────────────────────────────────────────────────────────────────

  private def f18Rows(n: Notice, d: F18ContractAwardDefence): List[String] =
    val obj          = d.contractObject
    val (tfV, tfC)   = totalFinalFields(obj.totalValue)
    val (cpvM, cpvA) = cpvFields(obj.cpv)
    val base = baseRow(n, "F18", d.contractingAuthority)
      .copy(
        contractType       = obj.contractType.fold("")(_.toString),
        contractTitle      = obj.title.fold("")(_.value),
        shortDescription   = obj.description.fold("")(_.value),
        cpvCode            = cpvM, cpvAdditional = cpvA,
        nutsCode           = nutsStr(obj.location),
        procedureType      = procedureTypeStr(d.procedure.procedureType),
        procJustification  = annexDStr(d.procedure.procedureType),
        totalFinalValue    = tfV, totalFinalCurrency = tfC,
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      )
    expandAwards(base, d.awards)

  // ── F19 ─────────────────────────────────────────────────────────────────────

  private def f19Row(n: Notice, d: F19ContractSubDefence): String =
    val obj       = d.contractObject
    val (cpvM, _) = cpvFields(obj.cpv)
    baseRow(n, "F19", d.contractingAuthority)
      .copy(
        contractTitle      = obj.title.fold("")(_.value),
        shortDescription   = obj.description.value,
        cpvCode            = cpvM,
        nutsCode           = nutsStr(obj.location),
        procedureType      = procedureTypeStr(d.procedure.procedureType),
        relatesToEuProject = d.complementaryInfo.relatesToEuProject.fold("")(_.toString)
      ).toCsvLine

  // ── T01 ─────────────────────────────────────────────────────────────────────

  private def t01Rows(n: Notice, d: T01PriorInformationMove): List[String] =
    val obj          = d.contractObject
    val (estV, estC) = valueAndCurrency(obj.estimatedValue)
    val (cpvM, cpvA) = cpvFields(obj.cpv)
    val base = baseRow(n, "T01", d.authority)
      .copy(
        contractType     = obj.contractType.fold("")(_.toString),
        contractTitle    = obj.title.fold("")(_.value),
        shortDescription = obj.description.fold("")(_.value),
        cpvCode          = cpvM, cpvAdditional = cpvA,
        nutsCode         = nutsStr(obj.location),
        procedureType    = d.procedure.procedureType.fold("")(_.value),
        estimatedValue   = estV, estimatedCurrency = estC
      )
    expandAwards(base, d.awards)

  // ── T02 ─────────────────────────────────────────────────────────────────────

  private def t02Rows(n: Notice, d: T02ContractMove): List[String] =
    val obj          = d.contractObject
    val (cpvM, cpvA) = cpvFields(obj.cpv)
    val base = baseRow(n, "T02", d.authority)
      .copy(
        contractTitle    = obj.title.fold("")(_.value),
        shortDescription = obj.description.fold("")(_.value),
        cpvCode          = cpvM, cpvAdditional = cpvA,
        nutsCode         = nutsStr(obj.location),
        procedureType    = d.procedure.procedureType.fold("")(_.value)
      )
    expandAwards(base, d.awards)

  // ── OthNot / Eeig ───────────────────────────────────────────────────────────

  private def othNotRow(n: Notice, d: OthNot): String =
    val cd = n.codedDataSection.codifData
    val nd = n.codedDataSection.noticeData
    Row(
      noticeOjId      = nd.noDocOjs.fold("")(_.value),
      noticeType      = "Other Notice",
      publicationDate = n.codedDataSection.refOjs.publicationDate.toIso,
      dispatchDate    = cd.dispatchDate.toIso,
      shortDescription = d.content.value
    ).toCsvLine

  private def eeigRow(n: Notice, d: Eeig): String =
    val cd = n.codedDataSection.codifData
    val nd = n.codedDataSection.noticeData
    Row(
      noticeOjId      = nd.noDocOjs.fold("")(_.value),
      noticeType      = "EEIG Notice",
      publicationDate = n.codedDataSection.refOjs.publicationDate.toIso,
      dispatchDate    = cd.dispatchDate.toIso
    ).toCsvLine
