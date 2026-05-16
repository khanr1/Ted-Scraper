package io.github.khanr1.tedscraper
package services.eforms

import fs2.Stream

import io.github.khanr1.tedscraper.eforms.*
import io.github.khanr1.tedscraper.repositories.eforms.EFormsNoticeRepository

trait EFormsNoticeService[F[_]]:
  def getAll: Stream[F, NoticeForm]
  def getBySubtype(subtype: NoticeSubtypeCode): Stream[F, NoticeForm]
  def getContractAwards: Stream[F, CanNotice]
  def getPriorInformation: Stream[F, PinNotice]
  def getContractNotices: Stream[F, CnNotice]
  def toCSV(notices: Stream[F, NoticeForm]): Stream[F, String]

object EFormsNoticeService:

  def make[F[_]](repo: EFormsNoticeRepository[F]): EFormsNoticeService[F] =
    new EFormsNoticeService[F]:
      def getAll = repo.getAll

      def getBySubtype(s: NoticeSubtypeCode): Stream[F, NoticeForm] =
        repo.getAll.filter(_.data.metadata.noticeTypeCode == s)

      def getContractAwards: Stream[F, CanNotice] =
        repo.getAll.collect { case n: CanNotice => n }

      def getPriorInformation: Stream[F, PinNotice] =
        repo.getAll.collect { case n: PinNotice => n }

      def getContractNotices: Stream[F, CnNotice] =
        repo.getAll.collect { case n: CnNotice => n }

      def toCSV(notices: Stream[F, NoticeForm]): Stream[F, String] =
        Stream.emit(EFormsNoticeService.csvHeader) ++
          notices.flatMap(n => Stream.emits(EFormsNoticeService.noticeToRows(n)))

  // ── CSV infrastructure — identical 34-column schema as r208Services / r209Services ──

  private[eforms] val csvHeader: String = List(
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

  // 34 fields — productIterator renders in declaration order, must match csvHeader
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

  // ── Field helpers ────────────────────────────────────────────────────────────

  // Build the OJ reference string from GazetteID ("NNN/YYYY") + NoticePublicationID ("NNNNNNNN-YYYY").
  // Produces "YYYY/S NNN-NNNNNN" matching the traditional TED format; falls back to NoticePublicationID alone.
  private def ojRef(meta: NoticeMetadata): String =
    (meta.gazetteId, meta.noticePublicationId) match
      case (Some(gaz), Some(pubId)) =>
        // GazetteID = "NNN/YYYY", NoticePublicationID = "NNNNNNNN-YYYY"
        val gazetteStr = gaz.value                      // e.g. "10/2025"
        val pubStr     = pubId.value                    // e.g. "00028660-2025"
        gazetteStr.split('/') match
          case Array(issue, year) =>
            // Strip leading zeros from the doc number (drop the trailing -YYYY)
            val docNum = pubStr.split('-').headOption.getOrElse(pubStr).replaceFirst("^0+", "")
            s"$year/S $issue-$docNum"
          case _ => pubStr
      case (None, Some(pubId)) => pubId.value
      case _                   => meta.noticeId.value   // last resort: UUID

  // BT-105 procurement-procedure-type codelist → human-readable label (mirrors r208/r209 style)
  private def procedureTypeLabel(code: Option[PlainText]): String = code.map(_.value) match
    case Some("open")         => "Open"
    case Some("restricted")   => "Restricted"
    case Some("neg-wo-call")  => "Negotiated without Call"
    case Some("neg-w-call")   => "Negotiated with Call"
    case Some("comp-dial")    => "Competitive Dialogue"
    case Some("comp-tend")    => "Competitive Tendering"
    case Some("innovation")   => "Innovation Partnership"
    case Some("oth-mult")     => "Other (Multiple)"
    case Some(other)          => other
    case None                 => ""

  private def noticeTypeLabel(nf: NoticeForm): String = nf match
    case _: PinNotice =>
      nf.noticeSubtype.value match
        case s if s.startsWith("pin-buyer")       => "Prior Information Notice (Buyer Profile)"
        case s if s.startsWith("pin-rtl")         => "Prior Information Notice (Reduced Time Limit)"
        case s if s.startsWith("pin-cfc-social")  => "Prior Information Notice (CfC Social)"
        case s if s.startsWith("pin-cfc")         => "Prior Information Notice (CfC)"
        case _                                     => "Prior Information Notice"
    case _: CnNotice =>
      nf.noticeSubtype.value match
        case s if s.startsWith("cn-social") => "Contract Notice (Social Services)"
        case s if s.startsWith("cn-desg")   => "Design Contest Notice"
        case _                               => "Contract Notice"
    case _: CanNotice =>
      nf.noticeSubtype.value match
        case "veat"                              => "Voluntary Ex Ante Transparency Notice"
        case s if s.startsWith("can-social")    => "Contract Award Notice (Social Services)"
        case s if s.startsWith("can-desg")      => "Contract Award Notice (Design Contest)"
        case s if s.startsWith("can-modif")     => "Contract Award Notice (Modification)"
        case _                                   => "Contract Award Notice"
    case _: UnknownNotice => nf.noticeSubtype.value

  // ── Row builder ──────────────────────────────────────────────────────────────

  private[eforms] def noticeToRows(nf: NoticeForm): List[String] =
    val n    = nf.data
    val meta = n.metadata

    // Organisation lookup map: orgId → Organization
    val orgMap = n.organizations.map(o => o.company.organizationId -> o).toMap

    // Buyer: first buyer → look up org
    val firstBuyer = n.buyers.headOption
    val buyerOrg   = firstBuyer.flatMap(b => orgMap.get(b.organizationRef))
    val caName     = buyerOrg.flatMap(_.company.name).map(_.value).getOrElse("")
    val caTown     = buyerOrg.flatMap(_.company.address).flatMap(_.city).map(_.value).getOrElse("")
    val caCountry  = buyerOrg.flatMap(_.company.address).flatMap(_.country).map(_.value).getOrElse("")
    val authType   = firstBuyer.flatMap(_.legalType).map(_.toString).getOrElse("")

    // Procedure-level scope (ND-ProcedureProcurementScope)
    val procScope = n.procedureScope

    // Procedure-level fields (fall back to first lot's scope if absent)
    val procContractType = procScope.flatMap(_.contractNature)
                             .orElse(n.lots.headOption.flatMap(_.scope.contractNature))
                             .map(_.toString).getOrElse("")
    val procTitle        = procScope.flatMap(_.lotTitle).map(_.value)
                             .orElse(n.lots.headOption.flatMap(_.scope.lotTitle).map(_.value))
                             .getOrElse("")
    val procDesc         = procScope.flatMap(_.lotDescription).map(_.value)
                             .orElse(n.lots.headOption.flatMap(_.scope.lotDescription).map(_.value))
                             .getOrElse("")
    val procCpv          = procScope.flatMap(_.mainCpvCode).map(_.value)
                             .orElse(n.lots.headOption.flatMap(_.scope.mainCpvCode).map(_.value))
                             .getOrElse("")

    // Notice-level base row
    val baseRow = Row(
      schemaVersion  = meta.sdkVersion.value,
      noticeOjId     = ojRef(meta),
      noticeType     = noticeTypeLabel(nf),
      publicationDate= meta.issueDate.map(_.value).getOrElse(""),
      dispatchDate   = "",   // efbc:TransmissionDate — not in current domain
      caName         = caName,
      caTown         = caTown,
      caCountry      = caCountry,
      authorityType  = authType,
      contractType   = procContractType,
      contractTitle  = procTitle,
      shortDescription = procDesc,
      cpvCode        = procCpv,
      cpvAdditional  = "",  // AdditionalCommodityClassification — not parsed
      procedureType  = procedureTypeLabel(n.procedureCode),
      awardCriteriaSummary = "", // BT-539 — not parsed
      procJustification    = n.procedureJustification.map(_.value).getOrElse(""),
      refNoticeNumber      = "", // not parsed
      relatesToEuProject   = ""  // not parsed
    )

    // Index maps for result section lookups
    val lotMap      = n.lots.map(l => l.lotId -> l).toMap
    val noticeResult= n.noticeResult

    noticeResult match
      case None =>
        // PIN or CN: one envelope row, no award columns
        List(baseRow.toCsvLine)

      case Some(nr) if nr.lotResults.isEmpty =>
        // CAN with no lot results: one row with notice totals
        List(baseRow.copy(
          totalFinalValue   = nr.totalAmount.map(_.value.toString).getOrElse(""),
          totalFinalCurrency= nr.currency.map(_.value).getOrElse("")
        ).toCsvLine)

      case Some(nr) =>
        val tenderMap   = nr.tenders.map(t => t.tenderId -> t).toMap
        val contractMap = nr.settledContracts.map(c => c.contractId -> c).toMap
        val tpMap       = nr.tenderingParties.map(tp => tp.tenderingPartyId -> tp).toMap

        nr.lotResults.map { lr =>
          val lot      = lr.lotRef.flatMap(lotMap.get)
          val scope    = lot.map(_.scope)
          val tender   = lr.tenderRef.flatMap(tenderMap.get)
          val contract = lr.contractRef.flatMap(contractMap.get)
          val tp       = tender.flatMap(_.tenderingPartyRef).flatMap(tpMap.get)
          val winOrg   = tp.flatMap(_.tendererOrgRefs.headOption).flatMap(orgMap.get)

          // Lot-level CPV and NUTS
          val lotCpv   = scope.flatMap(_.mainCpvCode).map(_.value).getOrElse("")
          val nutsCode = scope.map(_.nutsCode.map(_.value).mkString(";")).getOrElse("")

          // Winner info: prefer TenderingParty.name, fall back to winner org
          val wName    = tp.flatMap(_.name).map(_.value)
                           .orElse(winOrg.flatMap(_.company.name).map(_.value)).getOrElse("")
          val wTown    = winOrg.flatMap(_.company.address).flatMap(_.city).map(_.value).getOrElse("")
          val wCountry = winOrg.flatMap(_.company.address).flatMap(_.country).map(_.value).getOrElse("")

          baseRow.copy(
            nutsCode           = nutsCode,
            lotNumber          = lr.lotRef.map(_.value).getOrElse(""),
            awardContractNumber= contract.flatMap(_.contractReference).map(_.value).getOrElse(""),
            awardContractTitle = scope.flatMap(_.lotTitle).map(_.value)
                                   .orElse(contract.flatMap(_.title).map(_.value)).getOrElse(""),
            lotDescription     = scope.flatMap(_.lotDescription).map(_.value).getOrElse(""),
            lotCpv             = lotCpv,
            awardDate          = contract.flatMap(_.awardDate).map(_.value).getOrElse(""),
            totalFinalValue    = tender.flatMap(_.payableAmount).map(_.value.toString)
                                   .orElse(nr.totalAmount.map(_.value.toString)).getOrElse(""),
            totalFinalCurrency = tender.flatMap(_.currency).map(_.value)
                                   .orElse(nr.currency.map(_.value)).getOrElse(""),
            offersReceived     = lr.offersReceived.fold("")(_.toString),
            winnerName         = wName,
            winnerTown         = wTown,
            winnerCountry      = wCountry
          ).toCsvLine
        }
