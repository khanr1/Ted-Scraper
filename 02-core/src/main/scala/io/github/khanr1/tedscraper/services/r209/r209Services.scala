package io.github.khanr1.tedscraper
package services.r209

import cats.effect.Async
import fs2.Stream

import r209.*
import io.github.khanr1.tedscraper.common.types.*

trait r209Services[F[_]]:
  def toCSV(notices: Stream[F, r209.Notice]): Stream[F, String]

object r209Services:

  def make[F[_]: Async](noticeRepo: repositories.r209.NoticeRepository[F]): r209Services[F] =
    new r209Services[F]:
      def toCSV(notices: Stream[F, r209.Notice]): Stream[F, String] =
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

  // ── Field helpers ───────────────────────────────────────────────────────────

  private def noticeTypeLabel(code: String): String = code match
    case "F01" => "Prior Information Notice"
    case "F02" => "Contract Notice"
    case "F03" => "Contract Award Notice"
    case "F04" => "Periodic Indicative Notice (Utilities)"
    case "F05" => "Contract Notice (Utilities)"
    case "F06" => "Contract Award Notice (Utilities)"
    case "F08" => "Buyer Profile"
    case "F12" => "Design Contest Notice"
    case "F13" => "Design Contest Result"
    case "F14" => "Corrigendum"
    case "F15" => "Voluntary Ex Ante Transparency Notice"
    case "F20" => "Modification Notice"
    case "F21" => "Social and Specific Services"
    case "F24" => "Concession Notice"
    case "F25" => "Concession Award Notice"
    case other  => other

  private def valStr(amt: Option[MonetaryAmount]): String =
    amt.fold("")(_.value.toString)

  private def curStr(cur: Option[Currency]): String =
    cur.fold("")(_.toString)

  private def nutsStr(nuts: List[NutsCode]): String =
    nuts.map(_.value).mkString(";")

  private def cpvMainStr(cpv: CpvCode): String = cpv.value

  private def cpvAddStr(cpvs: List[CpvCode]): String =
    cpvs.map(_.value).mkString(";")

  private def procedureTypeStr(pt: Option[ProcedureType2014]): String = pt match
    case None                                    => ""
    case Some(ProcedureType2014.Open)            => "Open"
    case Some(ProcedureType2014.Restricted)      => "Restricted"
    case Some(ProcedureType2014.CompetitiveNegotiation) => "Competitive Negotiation"
    case Some(ProcedureType2014.CompetitiveDialogue)    => "Competitive Dialogue"
    case Some(ProcedureType2014.InnovationPartnership)  => "Innovation Partnership"
    case Some(ProcedureType2014.NegotiatedWithPriorCall) => "Negotiated with Prior Call"
    case Some(ProcedureType2014.AwardWithoutCall)       => "Award without Prior Call"
    case Some(ProcedureType2014.AwardWithoutPublication) => "Award without Publication"
    case Some(ProcedureType2014.Unknown(raw))            => raw

  private def awardCriteriaStr(criteria: Option[AwardCriteria]): String =
    criteria match
      case None                                   => ""
      case Some(c) if c.lowestPrice               => "Lowest price"
      case Some(c) if c.qualityCriteria.nonEmpty  =>
        "MEAT: " + c.qualityCriteria.map(q =>
          s"${q.name.value}${q.weight.fold("")(w => s" ${w.value}%")}").mkString("; ")
      case Some(c) if c.costCriteria.nonEmpty     =>
        c.costCriteria.map(_.name.value).mkString("; ")
      case _                                      => ""

  // ── Base row: envelope + contracting body ───────────────────────────────────

  private def baseRow(n: r209.Notice, formType: String, cb: ContractingBody): Row =
    val cd  = n.codedDataSection.codifData
    val nd  = n.codedDataSection.noticeData
    // Use additional (on-behalf) address if present; otherwise use primary address
    val eff = cb.additionalAddresses.headOption.getOrElse(cb.address)
    Row(
      schemaVersion   = n.schemaVersion.value,
      noticeOjId      = nd.noDocOjs.fold("")(_.value),
      noticeType      = noticeTypeLabel(formType),
      publicationDate = n.codedDataSection.refOjs.publicationDate.toIso,
      dispatchDate    = cd.dispatchDate.toIso,
      caName          = eff.officialName.fold("")(_.value),
      caTown          = eff.town.fold("")(_.value),
      caCountry       = eff.country.fold("")(_.toString),
      authorityType   = cd.authorityType.label.value,
      refNoticeNumber = nd.refNotice.fold("")(_.noticeNumbers.map(_.value).mkString(";"))
    )

  // ── Award helpers ───────────────────────────────────────────────────────────

  private def withAward(row: Row, item: AwardContractItem): Row =
    item.awarded match
      case None => row.copy(
        awardContractNumber = item.contractNo.fold("")(_.value),
        awardContractTitle  = item.title.fold("")(_.value),
        lotNumber           = item.lotNo.fold("")(_.value)
      )
      case Some(a) =>
        val winner = a.contractors.headOption.map(_.address)
        row.copy(
          lotNumber           = item.lotNo.fold("")(_.value),
          awardContractNumber = item.contractNo.fold("")(_.value),
          awardContractTitle  = item.title.fold("")(_.value),
          awardDate           = a.dateConclusion.getOrElse(""),
          totalFinalValue     = valStr(a.valTotal.orElse(a.valRangeLow)),
          totalFinalCurrency  = curStr(a.valTotalCurrency.orElse(a.valRangeCurrency)),
          offersReceived      = a.tendersReceived.fold("")(_.toString),
          winnerName          = winner.flatMap(_.officialName).fold("")(_.value),
          winnerTown          = winner.flatMap(_.town).fold("")(_.value),
          winnerCountry       = winner.flatMap(_.country).fold("")(_.toString)
        )

  private def expandAwards(base: Row, awards: List[AwardContractItem]): List[String] =
    if awards.isEmpty then List(base.toCsvLine)
    else awards.map(a => withAward(base, a).toCsvLine)

  // ── Top-level dispatch ──────────────────────────────────────────────────────

  private def noticeToRows(n: r209.Notice): List[String] =
    n.formSection.forms.flatMap(formToRows(n, _))

  private def formToRows(n: r209.Notice, fb: FormBody): List[String] = fb match
    case f: F01PriorInfo2014               => f01Rows(n, f)
    case f: F02ContractNotice2014          => f02Rows(n, f)
    case f: F03ContractAward2014           => f03Rows(n, f)
    case f: F04PeriodicIndicative2014      => f04Rows(n, f)
    case f: F05ContractNoticeUtilities2014 => f05Rows(n, f)
    case f: F06ContractAwardUtilities2014  => f06Rows(n, f)
    case f: F08BuyerProfile2014            => List(f08Row(n, f))
    case f: F12DesignContest2014           => List(f12Row(n, f))
    case f: F13DesignContestResult2014     => List(f13Row(n, f))
    case f: F14Corrigendum2014             => List(f14Row(n, f))
    case f: F15Veat2014                    => f15Rows(n, f)
    case f: F20Modification2014            => f20Rows(n, f)
    case f: F21SocialServices2014          => f21Rows(n, f)
    case f: F24ConcessionNotice2014        => List(f24Row(n, f))
    case f: F25ConcessionAward2014         => f25Rows(n, f)
    case f: Move2014                       => List(moveRow(n, f))
    case _: UnknownForm2014 => Nil

  // ── F01 ─────────────────────────────────────────────────────────────────────

  private def f01Rows(n: r209.Notice, f: F01PriorInfo2014): List[String] =
    val oc = f.objectContracts.headOption
    val base = baseRow(n, "F01", f.contractingBody).copy(
      contractType   = oc.flatMap(_.contractType).getOrElse(""),
      contractTitle  = oc.fold("")(_.title.value),
      shortDescription = oc.flatMap(_.shortDescr).fold("")(_.value),
      cpvCode        = oc.fold("")(o => cpvMainStr(o.cpvMain)),
      estimatedValue = valStr(oc.flatMap(_.valEstimatedTotal)),
      estimatedCurrency = curStr(oc.flatMap(_.valEstimatedCurrency))
    )
    val lots = oc.fold(List.empty[ObjectDescr])(_.lots)
    if lots.isEmpty then List(base.toCsvLine)
    else lots.map { lot =>
      base.copy(
        lotNumber     = lot.item.toString,
        awardContractTitle = lot.title.fold("")(_.value),
        lotDescription = lot.shortDescr.fold("")(_.value),
        lotCpv        = cpvAddStr(lot.cpvAdditional),
        nutsCode      = nutsStr(lot.nuts)
      ).toCsvLine
    }

  // ── F02 ─────────────────────────────────────────────────────────────────────

  private def f02Rows(n: r209.Notice, f: F02ContractNotice2014): List[String] =
    val oc   = f.objectContract
    val lots = oc.lots
    val ac   = lots.headOption.flatMap(_.awardCriteria)
    val base = baseRow(n, "F02", f.contractingBody).copy(
      contractType     = oc.contractType.getOrElse(""),
      contractTitle    = oc.title.value,
      shortDescription = oc.shortDescr.fold("")(_.value),
      cpvCode          = cpvMainStr(oc.cpvMain),
      nutsCode         = nutsStr(lots.headOption.fold(Nil)(_.nuts)),
      procedureType    = procedureTypeStr(f.procedure.procedureType),
      procJustification = f.procedure.justification.fold("")(_.value),
      awardCriteriaSummary = awardCriteriaStr(ac),
      estimatedValue   = valStr(oc.valEstimatedTotal),
      estimatedCurrency = curStr(oc.valEstimatedCurrency),
      refNoticeNumber  = f.procedure.noticeNumberOj.getOrElse("")
    )
    if lots.isEmpty then List(base.toCsvLine)
    else lots.map { lot =>
      base.copy(
        lotNumber     = lot.item.toString,
        awardContractTitle = lot.title.fold("")(_.value),
        lotDescription = lot.shortDescr.fold("")(_.value),
        lotCpv        = cpvAddStr(lot.cpvAdditional),
        nutsCode      = nutsStr(lot.nuts),
        awardCriteriaSummary = awardCriteriaStr(lot.awardCriteria),
        estimatedValue = valStr(lot.valObject.orElse(oc.valEstimatedTotal)),
        estimatedCurrency = curStr(lot.valObjectCurrency.orElse(oc.valEstimatedCurrency))
      ).toCsvLine
    }

  // ── F03 ─────────────────────────────────────────────────────────────────────

  private def f03Rows(n: r209.Notice, f: F03ContractAward2014): List[String] =
    val oc = f.objectContract
    val base = baseRow(n, "F03", f.contractingBody).copy(
      contractType      = oc.contractType.getOrElse(""),
      contractTitle     = oc.title.value,
      shortDescription  = oc.shortDescr.fold("")(_.value),
      cpvCode           = cpvMainStr(oc.cpvMain),
      cpvAdditional     = cpvAddStr(oc.lots.flatMap(_.cpvAdditional)),
      nutsCode          = nutsStr(oc.lots.headOption.fold(Nil)(_.nuts)),
      procedureType     = procedureTypeStr(f.procedure.procedureType),
      procJustification = f.procedure.justification.fold("")(_.value),
      refNoticeNumber   = f.procedure.noticeNumberOj.getOrElse(""),
      totalFinalValue   = valStr(oc.valTotal),
      totalFinalCurrency = curStr(oc.valTotalCurrency),
      estimatedValue    = valStr(oc.valEstimatedTotal),
      estimatedCurrency = curStr(oc.valEstimatedCurrency)
    )
    expandAwards(base, f.awards)

  // ── F04 ─────────────────────────────────────────────────────────────────────

  private def f04Rows(n: r209.Notice, f: F04PeriodicIndicative2014): List[String] =
    val base = baseRow(n, "F04", f.contractingBody)
    val rows = f.objectContracts.flatMap { oc =>
      val b = base.copy(
        contractType = oc.contractType.getOrElse(""),
        contractTitle = oc.title.value,
        shortDescription = oc.shortDescr.fold("")(_.value),
        cpvCode = cpvMainStr(oc.cpvMain),
        estimatedValue = valStr(oc.valEstimatedTotal),
        estimatedCurrency = curStr(oc.valEstimatedCurrency)
      )
      if oc.lots.isEmpty then List(b)
      else oc.lots.map { lot =>
        b.copy(
          lotNumber = lot.item.toString,
          lotDescription = lot.shortDescr.fold("")(_.value),
          nutsCode = nutsStr(lot.nuts)
        )
      }
    }
    if rows.isEmpty then List(base.toCsvLine) else rows.map(_.toCsvLine)

  // ── F05 ─────────────────────────────────────────────────────────────────────

  private def f05Rows(n: r209.Notice, f: F05ContractNoticeUtilities2014): List[String] =
    f02Rows(n, F02ContractNotice2014(f.meta, f.legalBasis, f.contractingBody,
      f.objectContract, f.lefti, f.procedure, f.complementaryInfo))

  // ── F06 ─────────────────────────────────────────────────────────────────────

  private def f06Rows(n: r209.Notice, f: F06ContractAwardUtilities2014): List[String] =
    f03Rows(n, F03ContractAward2014(f.meta, f.legalBasis, f.contractingBody,
      f.objectContract, f.procedure, f.awards, f.complementaryInfo))

  // ── F08 ─────────────────────────────────────────────────────────────────────

  private def f08Row(n: r209.Notice, f: F08BuyerProfile2014): String =
    baseRow(n, "F08", f.contractingBody).copy(
      contractTitle    = f.objectContract.title.value,
      shortDescription = f.objectContract.shortDescr.fold("")(_.value),
      cpvCode          = cpvMainStr(f.objectContract.cpvMain)
    ).toCsvLine

  // ── F12 ─────────────────────────────────────────────────────────────────────

  private def f12Row(n: r209.Notice, f: F12DesignContest2014): String =
    baseRow(n, "F12", f.contractingBody).copy(
      contractTitle    = f.objectContract.title.value,
      shortDescription = f.objectContract.shortDescr.fold("")(_.value),
      cpvCode          = cpvMainStr(f.objectContract.cpvMain)
    ).toCsvLine

  // ── F13 ─────────────────────────────────────────────────────────────────────

  private def f13Row(n: r209.Notice, f: F13DesignContestResult2014): String =
    baseRow(n, "F13", f.contractingBody).copy(
      contractTitle    = f.objectContract.title.value,
      shortDescription = f.objectContract.shortDescr.fold("")(_.value),
      cpvCode          = cpvMainStr(f.objectContract.cpvMain)
    ).toCsvLine

  // ── F14 ─────────────────────────────────────────────────────────────────────

  private def f14Row(n: r209.Notice, f: F14Corrigendum2014): String =
    baseRow(n, "F14", f.contractingBody).copy(
      contractTitle    = f.objectContract.title.value,
      shortDescription = f.complementaryInfo.infoAdd.fold("")(_.value)
    ).toCsvLine

  // ── F15 ─────────────────────────────────────────────────────────────────────

  private def f15Rows(n: r209.Notice, f: F15Veat2014): List[String] =
    val oc = f.objectContract
    val base = baseRow(n, "F15", f.contractingBody).copy(
      contractType      = oc.contractType.getOrElse(""),
      contractTitle     = oc.title.value,
      shortDescription  = oc.shortDescr.fold("")(_.value),
      cpvCode           = cpvMainStr(oc.cpvMain),
      nutsCode          = nutsStr(oc.lots.headOption.fold(Nil)(_.nuts)),
      procedureType     = procedureTypeStr(f.procedure.procedureType),
      procJustification = f.procedure.justification.fold("")(_.value),
      refNoticeNumber   = f.procedure.noticeNumberOj.getOrElse(""),
      totalFinalValue   = valStr(oc.valTotal),
      totalFinalCurrency = curStr(oc.valTotalCurrency),
      estimatedValue    = valStr(oc.valEstimatedTotal),
      estimatedCurrency = curStr(oc.valEstimatedCurrency)
    )
    expandAwards(base, f.awards)

  // ── F20 ─────────────────────────────────────────────────────────────────────

  private def f20Rows(n: r209.Notice, f: F20Modification2014): List[String] =
    val base = baseRow(n, "F20", f.contractingBody).copy(
      contractTitle    = f.objectContract.title.value,
      shortDescription = f.objectContract.shortDescr.fold("")(_.value),
      cpvCode          = cpvMainStr(f.objectContract.cpvMain)
    )
    f.award.fold(List(base.toCsvLine))(a => List(withAward(base, a).toCsvLine))

  // ── F21 ─────────────────────────────────────────────────────────────────────

  private def f21Rows(n: r209.Notice, f: F21SocialServices2014): List[String] =
    val oc = f.objectContracts.headOption
    val base = baseRow(n, "F21", f.contractingBody).copy(
      contractTitle    = oc.fold("")(_.title.value),
      shortDescription = oc.flatMap(_.shortDescr).fold("")(_.value),
      cpvCode          = oc.fold("")(o => cpvMainStr(o.cpvMain))
    )
    expandAwards(base, f.awards)

  // ── F24 ─────────────────────────────────────────────────────────────────────

  private def f24Row(n: r209.Notice, f: F24ConcessionNotice2014): String =
    baseRow(n, "F24", f.contractingBody).copy(
      contractTitle    = f.objectContract.title.value,
      shortDescription = f.objectContract.shortDescr.fold("")(_.value),
      cpvCode          = cpvMainStr(f.objectContract.cpvMain)
    ).toCsvLine

  // ── F25 ─────────────────────────────────────────────────────────────────────

  private def f25Rows(n: r209.Notice, f: F25ConcessionAward2014): List[String] =
    val base = baseRow(n, "F25", f.contractingBody).copy(
      contractTitle    = f.objectContract.title.value,
      shortDescription = f.objectContract.shortDescr.fold("")(_.value),
      cpvCode          = cpvMainStr(f.objectContract.cpvMain)
    )
    expandAwards(base, f.awards)

  // ── MOVE ─────────────────────────────────────────────────────────────────────

  private def moveRow(n: r209.Notice, f: Move2014): String =
    baseRow(n, "MOVE", f.contractingBody).copy(
      contractTitle    = f.objectContract.title.value,
      shortDescription = f.objectContract.shortDescr.fold("")(_.value),
      cpvCode          = cpvMainStr(f.objectContract.cpvMain)
    ).toCsvLine
