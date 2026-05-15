package io.github.khanr1.tedscraper.eforms.parser

import scala.xml.*
import scala.util.Try

import io.github.khanr1.tedscraper.eforms.*

object EFormsXmlParser:

  // ── Namespace URIs ───────────────────────────────────────────────────────────

  private val CBC  = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"
  private val CAC  = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
  private val EFBC = "http://data.europa.eu/p27/eforms-ubl-extension-basic-components/1"
  private val EFAC = "http://data.europa.eu/p27/eforms-ubl-extension-aggregate-components/1"
  private val EFEXT= "http://data.europa.eu/p27/eforms-ubl-extensions/1"

  // ── Low-level namespace-aware helpers ────────────────────────────────────────

  /** Direct children of every node in `n` with matching namespace + local name. */
  private def child(n: NodeSeq, ns: String, local: String): NodeSeq =
    n.flatMap(node => node.child.collect {
      case e: Elem if e.label == local && e.namespace == ns => e
    })

  /** Collapse XML whitespace (newlines + indentation) to a single space, then trim. */
  private def norm(s: String): String = s.replaceAll("\\s+", " ").trim

  /** Text content of the first matching child, whitespace-normalised. */
  private def txt(n: NodeSeq, ns: String, local: String): String =
    child(n, ns, local).headOption.map(n => norm(n.text)).getOrElse("")

  /** Text content as Some if non-empty. */
  private def opt(n: NodeSeq, ns: String, local: String): Option[String] =
    val v = txt(n, ns, local)
    if v.isEmpty then None else Some(v)

  /** Attribute value by name on the first node in n. */
  private def atr(n: NodeSeq, attrName: String): String =
    n.headOption.map(_ \@ attrName).getOrElse("")

  /** cbc:ID with optional schemeName filter.
   *  Newer SDKs use cbc:ID[@schemeName='result']; older SDKs emit bare cbc:ID.
   *  Falls back to any cbc:ID when the scheme-specific one is absent. */
  private def idWithScheme(parent: NodeSeq, scheme: String): String =
    val ids = child(parent, CBC, "ID")
    ids.find(_ \@ "schemeName" == scheme).orElse(ids.headOption)
      .map(n => norm(n.text)).getOrElse("")

  /** All descendant elements with matching namespace + local name (recursive). */
  private def deep(n: NodeSeq, ns: String, local: String): NodeSeq =
    (n \\ local).filter(e => Option(e.namespace).contains(ns))

  /** All direct children as Seq[Node] matching namespace + local name. */
  private def elmts(n: NodeSeq, ns: String, local: String): Seq[Node] =
    n.flatMap(node => node.child.collect {
      case e: Elem if e.label == local && e.namespace == ns => e
    }).toSeq

  // ── Extension helper: navigate to efext:EformsExtension ─────────────────────

  private def eformsExt(root: Elem): NodeSeq =
    (root \\ "EformsExtension").filter(e => Option(e.namespace).contains(EFEXT))

  // ── Iron-safe constructors used throughout the parser ────────────────────────

  private def safeId[T](raw: String, make: String => T, field: String, ctx: String)
                        (using ev: String => Either[String, T] = (_: String) => Left("not used"))
      : Either[ParseError, T] =
    if raw.nonEmpty then Right(make(raw))
    else Left(ParseError.MissingRequiredField(field, ctx))

  /** Build a non-empty typed ID from raw text, or return MissingRequiredField. */
  private def requireNonEmpty(raw: String, field: String, ctx: String): Either[ParseError, String] =
    if raw.nonEmpty then Right(raw) else Left(ParseError.MissingRequiredField(field, ctx))

  // ── ND-Root → NoticeMetadata ─────────────────────────────────────────────────

  private def parseNoticeMetadata(root: Elem): Either[ParseError, NoticeMetadata] =
    val noticeIdRaw = (root \ "ID")
      .filter(n => atr(n, "schemeName") == "notice-id")
      .headOption
      .map(_.text.trim)
      .getOrElse("")
    for
      noticeId         <- requireNonEmpty(noticeIdRaw, "BT-701/cbc:ID[@schemeName='notice-id']", "ND-Root")
                           .map(NoticeId.unsafe)
      sdkVersionRaw     = txt(root, CBC, "CustomizationID")
      sdkVersion       <- requireNonEmpty(sdkVersionRaw, "OPT-002/cbc:CustomizationID", "ND-Root")
                           .map(SdkVersion.unsafe)
      noticeTypeRaw     = txt(root, CBC, "NoticeTypeCode")
      noticeSubtype    <- requireNonEmpty(noticeTypeRaw, "BT-02/cbc:NoticeTypeCode", "ND-Root")
                           .map(NoticeSubtypeCode.unsafe)
    yield
      val cfRaw   = txt(root, CBC, "ContractFolderID")
      val dateRaw = txt(root, CBC, "IssueDate")
      val timeRaw = txt(root, CBC, "IssueTime")
      val langRaw = txt(root, CBC, "NoticeLanguageCode")
      val regRaw  = txt(root, CBC, "RegulatoryDomain")
      val verRaw  = txt(root, CBC, "VersionID")

      // efac:Publication — OJ publication reference (inside eforms extension)
      val pub    = child(eformsExt(root), EFAC, "Publication")
      val pubId  = child(pub, EFBC, "NoticePublicationID")
                     .filter(n => atr(n, "schemeName") == "ojs-notice-id")
                     .headOption.map(_.text.trim).filter(_.nonEmpty)
                     .map(PlainText.unsafe)
      val gazId  = child(pub, EFBC, "GazetteID")
                     .headOption.map(_.text.trim).filter(_.nonEmpty)
                     .map(PlainText.unsafe)

      NoticeMetadata(
        noticeId            = noticeId,
        sdkVersion          = sdkVersion,
        contractFolderId    = Option(cfRaw).filter(_.nonEmpty).map(ContractFolderId.unsafe),
        issueDate           = Option(dateRaw).filter(_.nonEmpty).flatMap(d => DateString.from(d).toOption),
        issueTime           = Option(timeRaw).filter(_.nonEmpty).flatMap(t => TimeString.from(t).toOption),
        noticeTypeCode      = noticeSubtype,
        language            = Option(langRaw).filter(_.nonEmpty).map(PlainText.unsafe),
        regulatoryDomain    = Option(regRaw).filter(_.nonEmpty).map(PlainText.unsafe),
        versionId           = Option(verRaw).filter(_.nonEmpty).map(PlainText.unsafe),
        noticePublicationId = pubId,
        gazetteId           = gazId
      )

  // ── ND-ContractingParty / ND-Buyer → BuyerRef ────────────────────────────────

  private def parseBuyer(cp: Node): Either[ParseError, BuyerRef] =
    val party    = child(NodeSeq.fromSeq(Seq(cp)), CAC, "Party")
    val idNode   = child(child(party, CAC, "PartyIdentification"), CBC, "ID")
    val orgIdRaw = idNode.headOption.map(_.text.trim).getOrElse("")
    requireNonEmpty(orgIdRaw, "OPT-300/cbc:ID", "ND-Buyer")
      .map(OrganizationId.unsafe)
      .map { orgId =>
        val legalTypeRaw =
          child(
            child(NodeSeq.fromSeq(Seq(cp)), CAC, "ContractingPartyType"),
            CBC, "PartyTypeCode"
          ).headOption.map(_.text.trim).getOrElse("")
        val profileUrl =
          child(NodeSeq.fromSeq(Seq(cp)), CBC, "BuyerProfileURI")
            .headOption.map(_.text.trim).filter(_.nonEmpty).map(UrlString.unsafe)
        BuyerRef(
          organizationRef = orgId,
          legalType       = Option(legalTypeRaw).filter(_.nonEmpty).map(BuyerLegalType.fromString),
          buyerProfileUrl = profileUrl
        )
      }

  // ── ND-Company postal address ─────────────────────────────────────────────────

  private def parseCompanyAddress(company: Node): Option[CompanyAddress] =
    val addr = child(NodeSeq.fromSeq(Seq(company)), CAC, "PostalAddress")
    if addr.isEmpty then None
    else
      val street  = opt(addr, CBC, "StreetName").map(PlainText.unsafe)
      val city    = opt(addr, CBC, "CityName").map(PlainText.unsafe)
      val postal  = opt(addr, CBC, "PostalZone").map(PlainText.unsafe)
      val nutsRaw = opt(addr, CBC, "CountrySubentityCode")
      val nuts    = nutsRaw.flatMap(r => NutsCode.from(r).toOption)
      val country = opt(child(addr, CAC, "Country"), CBC, "IdentificationCode").map(PlainText.unsafe)
      Some(CompanyAddress(street, city, postal, nuts, country))

  // ── ND-Company ────────────────────────────────────────────────────────────────

  private def parseCompany(orgNode: Node): Either[ParseError, Company] =
    val company = child(NodeSeq.fromSeq(Seq(orgNode)), EFAC, "Company")
    val idRaw   = child(child(company, CAC, "PartyIdentification"), CBC, "ID")
                    .headOption.map(_.text.trim).getOrElse("")
    requireNonEmpty(idRaw, "OPT-200/cbc:ID", "ND-Company")
      .map(OrganizationId.unsafe)
      .map { orgId =>
        val name    = opt(child(company, CAC, "PartyName"), CBC, "Name").map(PlainText.unsafe)
        val website = opt(company, CBC, "WebsiteURI").map(UrlString.unsafe)
        val endpoint= opt(company, CBC, "EndpointID").map(UrlString.unsafe)
        val contact = child(company, CAC, "Contact")
        val phone   = opt(contact, CBC, "Telephone").map(PlainText.unsafe)
        val email   = opt(contact, CBC, "ElectronicMail").map(PlainText.unsafe)
        val address = parseCompanyAddress(company.headOption.getOrElse(orgNode))
        Company(orgId, name, website, endpoint, address, phone, email)
      }

  // ── ND-Organization ───────────────────────────────────────────────────────────

  private def parseOrganization(orgNode: Node): Either[ParseError, Organization] =
    parseCompany(orgNode).map { company =>
      def boolField(local: String): Option[Boolean] =
        child(NodeSeq.fromSeq(Seq(orgNode)), EFBC, local)
          .headOption.map(_.text.trim.toLowerCase == "true")
      Organization(
        company        = company,
        naturalPerson  = boolField("NaturalPersonIndicator"),
        listedOnMarket = boolField("ListedOnRegulatedMarketIndicator"),
        isGroupLead    = boolField("GroupLeadIndicator"),
        isAwardingCPB  = boolField("AwardingCPBIndicator"),
        isAcquiringCPB = boolField("AcquiringCPBIndicator")
      )
    }

  // ── ND-LotProcurementScope → LotScope ─────────────────────────────────────────

  private def parseLotScope(project: Node): LotScope =
    val pSeq = NodeSeq.fromSeq(Seq(project))
    val title = opt(pSeq, CBC, "Name").map(PlainText.unsafe)
    val desc  = opt(pSeq, CBC, "Description").map(PlainText.unsafe)
    val addl  = opt(pSeq, CBC, "Note").map(PlainText.unsafe)

    val contractNatureRaw =
      child(pSeq, CAC, "ProcurementAdditionalType")
        .filter(n => atr(n \ "ProcurementTypeCode", "listName") == "contract-nature")
        .headOption
        .flatMap(n => opt(NodeSeq.fromSeq(Seq(n)), CBC, "ProcurementTypeCode"))
    val contractNature = contractNatureRaw.map(ContractNature.fromString)

    val cpvRaw =
      opt(child(child(pSeq, CAC, "MainCommodityClassification"), CBC, "ItemClassificationCode"), CBC, "")
      .orElse(
        child(pSeq, CAC, "MainCommodityClassification")
          .flatMap(_.child)
          .filter(n => n.label == "ItemClassificationCode" && n.namespace == CBC)
          .headOption
          .map(_.text.trim)
      )
      .orElse(
        child(pSeq, CAC, "MainCommodityClassification")
          .headOption
          .map(n => (n \ "ItemClassificationCode").headOption.map(_.text.trim).getOrElse(""))
          .filter(_.nonEmpty)
      )
    val cpvCode = cpvRaw.filter(_.nonEmpty).flatMap(r => CpvCode.from(r).toOption)

    val nuts = child(pSeq, CAC, "RealizedLocation")
      .flatMap(loc => child(NodeSeq.fromSeq(Seq(loc)), CAC, "Address"))
      .flatMap(addr => child(NodeSeq.fromSeq(Seq(addr)), CBC, "CountrySubentityCode"))
      .flatMap(n => NutsCode.from(n.text.trim).toOption)
      .toList

    LotScope(title, desc, contractNature, cpvCode, addl, nuts)

  // ── ND-LotTenderingProcess → LotProcess ───────────────────────────────────────

  private def parseLotProcess(lot: Node): Option[LotProcess] =
    val proc = child(NodeSeq.fromSeq(Seq(lot)), CAC, "TenderingProcess")
    if proc.isEmpty then None
    else
      val deadline = child(child(proc, CAC, "TenderSubmissionDeadlinePeriod"), CBC, "EndDate")
                       .headOption.map(_.text.trim).filter(_.nonEmpty)
                       .flatMap(d => DateString.from(d).toOption)
      Some(LotProcess(deadline))

  // ── ND-Lot ────────────────────────────────────────────────────────────────────

  private def parseLot(lot: Node): Either[ParseError, Lot] =
    val lotIdRaw = child(NodeSeq.fromSeq(Seq(lot)), CBC, "ID")
                     .filter(n => atr(n, "schemeName") == "Lot")
                     .headOption.map(_.text.trim).getOrElse("")
    requireNonEmpty(lotIdRaw, "BT-137/cbc:ID[@schemeName='Lot']", "ND-Lot")
      .map(LotId.unsafe)
      .map { lotId =>
        val project = child(NodeSeq.fromSeq(Seq(lot)), CAC, "ProcurementProject")
                        .headOption.getOrElse(lot)
        Lot(
          lotId   = lotId,
          scope   = parseLotScope(project),
          process = parseLotProcess(lot),
          terms   = Some(LotTerms())
        )
      }

  // ── ND-LotsGroup ──────────────────────────────────────────────────────────────

  private def parseLotsGroup(lg: Node): Either[ParseError, LotsGroup] =
    val idRaw = child(NodeSeq.fromSeq(Seq(lg)), CBC, "ID")
                  .filter(n => atr(n, "schemeName") == "LotsGroup")
                  .headOption.map(_.text.trim).getOrElse("")
    requireNonEmpty(idRaw, "cbc:ID[@schemeName='LotsGroup']", "ND-LotsGroup")
      .map(r => LotsGroup(LotId.unsafe(r)))

  // ── ND-Part ───────────────────────────────────────────────────────────────────

  private def parsePart(pt: Node): Either[ParseError, Part] =
    val idRaw = child(NodeSeq.fromSeq(Seq(pt)), CBC, "ID")
                  .filter(n => atr(n, "schemeName") == "Part")
                  .headOption.map(_.text.trim).getOrElse("")
    requireNonEmpty(idRaw, "cbc:ID[@schemeName='Part']", "ND-Part")
      .map(LotId.unsafe)
      .map { partId =>
        val proj  = child(NodeSeq.fromSeq(Seq(pt)), CAC, "ProcurementProject")
        val title = opt(proj, CBC, "Name").map(PlainText.unsafe)
        val desc  = opt(proj, CBC, "Description").map(PlainText.unsafe)
        Part(partId, title, desc)
      }

  // ── ND-SettledContract ────────────────────────────────────────────────────────

  private def parseSettledContract(sc: Node): Either[ParseError, SettledContract] =
    val scSeq  = NodeSeq.fromSeq(Seq(sc))
    val idRaw  = idWithScheme(scSeq, "contract")
    requireNonEmpty(idRaw, "OPT-316/cbc:ID[@schemeName='contract']", "ND-SettledContract")
      .map(ContractId.unsafe)
      .map { contractId =>
        val title      = opt(scSeq, CBC, "Title").map(PlainText.unsafe)
        val issueDate  = opt(scSeq, CBC, "IssueDate").flatMap(d => DateString.from(d).toOption)
        val awardDate  = opt(scSeq, CBC, "AwardDate").flatMap(d => DateString.from(d).toOption)
        val contractRef= opt(child(scSeq, EFAC, "ContractReference"), CBC, "ID").map(PlainText.unsafe)
        val tenderRef  = child(scSeq, EFAC, "LotTender")
                           .flatMap(lt => child(NodeSeq.fromSeq(Seq(lt)), CBC, "ID"))
                           .headOption.map(_.text.trim).filter(_.nonEmpty).map(TenderId.unsafe)
        val signIds    = child(scSeq, CAC, "SignatoryParty")
                           .flatMap(sp =>
                             child(child(NodeSeq.fromSeq(Seq(sp)), CAC, "PartyIdentification"), CBC, "ID")
                           )
                           .map(_.text.trim).filter(_.nonEmpty).map(OrganizationId.unsafe).toList
        SettledContract(contractId, title, issueDate, awardDate, contractRef, tenderRef, signIds)
      }

  // ── ND-LotTender (inside NoticeResult) ───────────────────────────────────────

  private def parseLotTender(lt: Node): Either[ParseError, LotTender] =
    val ltSeq  = NodeSeq.fromSeq(Seq(lt))
    val idRaw  = idWithScheme(ltSeq, "tender")
    requireNonEmpty(idRaw, "cbc:ID[@schemeName='tender']", "ND-LotTender")
      .map(TenderId.unsafe)
      .map { tenderId =>
        val payAmtNode = child(child(ltSeq, CAC, "LegalMonetaryTotal"), CBC, "PayableAmount")
        val payAmt     = payAmtNode.headOption.map(_.text.trim).filter(_.nonEmpty)
                           .flatMap(s => scala.util.Try(s.toDouble).toOption)
                           .flatMap(d => Amount.from(d).toOption)
        val currency   = payAmtNode.headOption.map(_ \@ "currencyID").filter(_.nonEmpty).map(PlainText.unsafe)
        val rankCode   = opt(ltSeq, CBC, "RankCode").map(PlainText.unsafe)
        val tpRef      = child(ltSeq, EFAC, "TenderingParty")
                           .flatMap(tp => child(NodeSeq.fromSeq(Seq(tp)), CBC, "ID"))
                           .headOption.map(_.text.trim).filter(_.nonEmpty).map(TenderingPartyId.unsafe)
        val lotRef     = child(ltSeq, EFAC, "TenderLot")
                           .flatMap(tl => child(NodeSeq.fromSeq(Seq(tl)), CBC, "ID"))
                           .headOption.map(_.text.trim).filter(_.nonEmpty).map(LotId.unsafe)
        LotTender(tenderId, payAmt, currency, rankCode, tpRef, lotRef)
      }

  // ── ND-TenderingParty ─────────────────────────────────────────────────────────

  private def parseTenderingParty(tp: Node): Either[ParseError, TenderingParty] =
    val tpSeq  = NodeSeq.fromSeq(Seq(tp))
    val idRaw  = idWithScheme(tpSeq, "tendering-party")
    requireNonEmpty(idRaw, "OPT-210/cbc:ID[@schemeName='tendering-party']", "ND-TenderingParty")
      .map(TenderingPartyId.unsafe)
      .map { tpId =>
        val name       = opt(tpSeq, CBC, "Name").map(PlainText.unsafe)
        val tenderers  = child(tpSeq, EFAC, "Tenderer")
                           .flatMap(t => child(NodeSeq.fromSeq(Seq(t)), CBC, "ID"))
                           .map(_.text.trim).filter(_.nonEmpty).map(OrganizationId.unsafe).toList
        TenderingParty(tpId, name, tenderers)
      }

  // ── ND-LotResult ──────────────────────────────────────────────────────────────

  private def parseLotResult(lr: Node): Either[ParseError, LotResult] =
    val lrSeq = NodeSeq.fromSeq(Seq(lr))
    val idRaw = idWithScheme(lrSeq, "result")
    requireNonEmpty(idRaw, "OPT-322/cbc:ID[@schemeName='result']", "ND-LotResult")
      .map(ResultId.unsafe)
      .map { resultId =>
        val code = opt(lrSeq, CBC, "TenderResultCode")
                     .map(WinnerSelectionStatus.fromString)
                     .getOrElse(WinnerSelectionStatus.Unknown)
        val lower = opt(lrSeq, CBC, "LowerTenderAmount")
                      .flatMap(s => scala.util.Try(s.toDouble).toOption)
                      .flatMap(d => Amount.from(d).toOption)
        val higher= opt(lrSeq, CBC, "HigherTenderAmount")
                      .flatMap(s => scala.util.Try(s.toDouble).toOption)
                      .flatMap(d => Amount.from(d).toOption)
        val tRef  = child(lrSeq, EFAC, "LotTender")
                      .flatMap(lt => child(NodeSeq.fromSeq(Seq(lt)), CBC, "ID"))
                      .headOption.map(_.text.trim).filter(_.nonEmpty).map(TenderId.unsafe)
        val cRef  = child(lrSeq, EFAC, "SettledContract")
                      .flatMap(sc => child(NodeSeq.fromSeq(Seq(sc)), CBC, "ID"))
                      .headOption.map(_.text.trim).filter(_.nonEmpty).map(ContractId.unsafe)
        val lotRef= child(lrSeq, EFAC, "TenderLot")
                      .flatMap(tl => child(NodeSeq.fromSeq(Seq(tl)), CBC, "ID"))
                      .headOption.map(_.text.trim).filter(_.nonEmpty).map(LotId.unsafe)
        // BT-759: total received submissions (StatisticsNumeric where no specific type code filter)
        val offers= child(lrSeq, EFAC, "ReceivedSubmissionsStatistics")
                      .flatMap(rss => child(NodeSeq.fromSeq(Seq(rss)), EFBC, "StatisticsNumeric"))
                      .headOption.map(_.text.trim)
                      .flatMap(s => scala.util.Try(s.toInt).toOption)
        LotResult(resultId, code, lower, higher, tRef, cRef, lotRef, offers)
      }

  // ── ND-NoticeResult ───────────────────────────────────────────────────────────

  private def parseNoticeResult(ext: NodeSeq): Either[ParseError, Option[NoticeResult]] =
    val nrNodes = child(ext, EFAC, "NoticeResult")
    if nrNodes.isEmpty then Right(None)
    else
      val nr          = nrNodes.head
      val nrSeq       = NodeSeq.fromSeq(Seq(nr))
      val totalAmtNode= child(nrSeq, CBC, "TotalAmount")
      val totalAmt    = totalAmtNode.headOption.map(_.text.trim).filter(_.nonEmpty)
                          .flatMap(s => scala.util.Try(s.toDouble).toOption)
                          .flatMap(d => Amount.from(d).toOption)
      val currency    = totalAmtNode.headOption.map(_ \@ "currencyID").filter(_.nonEmpty).map(PlainText.unsafe)

      val lotResultsE   = elmts(nrSeq, EFAC, "LotResult").map(parseLotResult)
      val tendersE      = elmts(nrSeq, EFAC, "LotTender").map(parseLotTender)
      val contractsE    = elmts(nrSeq, EFAC, "SettledContract").map(parseSettledContract)
      val tPartyE       = elmts(nrSeq, EFAC, "TenderingParty").map(parseTenderingParty)

      def collectAll[T](es: Seq[Either[ParseError, T]]): Either[ParseError, List[T]] =
        es.foldRight(Right(Nil): Either[ParseError, List[T]]) {
          case (Right(v), Right(acc)) => Right(v :: acc)
          case (Left(e), _)           => Left(e)
          case (_, Left(e))           => Left(e)
        }

      for
        lotResults   <- collectAll(lotResultsE)
        tenders      <- collectAll(tendersE)
        contracts    <- collectAll(contractsE)
        tParties     <- collectAll(tPartyE)
      yield
        Some(NoticeResult(totalAmt, currency, lotResults, tenders, contracts, tParties))

  // ── Full notice assembly ──────────────────────────────────────────────────────

  private def parseNotice(root: Elem): Either[ParseError, Notice] =
    val ext      = eformsExt(root)

    // ND-ContractingParty (all buyers at root level)
    val buyersE  = elmts(NodeSeq.fromSeq(Seq(root)), CAC, "ContractingParty").map(parseBuyer)

    // ND-Organization (inside efac:Organizations)
    val orgsE    = child(ext, EFAC, "Organizations")
                     .toSeq.flatMap(o => elmts(NodeSeq.fromSeq(Seq(o)), EFAC, "Organization"))
                     .map(parseOrganization)

    // Lots / LotsGroups / Parts — all via cac:ProcurementProjectLot with @schemeName
    val projectLots = elmts(NodeSeq.fromSeq(Seq(root)), CAC, "ProcurementProjectLot")
    val lotsE    = projectLots
                     .filter(n => atr(child(NodeSeq.fromSeq(Seq(n)), CBC, "ID"), "schemeName") == "Lot")
                     .map(parseLot)
    val groupsE  = projectLots
                     .filter(n => atr(child(NodeSeq.fromSeq(Seq(n)), CBC, "ID"), "schemeName") == "LotsGroup")
                     .map(parseLotsGroup)
    val partsE   = projectLots
                     .filter(n => atr(child(NodeSeq.fromSeq(Seq(n)), CBC, "ID"), "schemeName") == "Part")
                     .map(parsePart)

    def collectAll[T](es: Seq[Either[ParseError, T]]): Either[ParseError, List[T]] =
      es.foldRight(Right(Nil): Either[ParseError, List[T]]) {
        case (Right(v), Right(acc)) => Right(v :: acc)
        case (Left(e), _)           => Left(e)
        case (_, Left(e))           => Left(e)
      }

    // ND-ProcedureProcurementScope: root-level cac:ProcurementProject (not inside a Lot)
    val procProject    = elmts(NodeSeq.fromSeq(Seq(root)), CAC, "ProcurementProject").headOption
    val procedureScope = procProject.map(parseLotScope)

    // ND-ProcedureTenderingProcess: root-level cac:TenderingProcess
    val tenderingProc  = elmts(NodeSeq.fromSeq(Seq(root)), CAC, "TenderingProcess").headOption
    val tpSeqOpt       = tenderingProc.map(tp => NodeSeq.fromSeq(Seq(tp)))

    // BT-105: cbc:ProcedureCode (procurement-procedure-type)
    val procedureCode  = tpSeqOpt
      .flatMap(tp => opt(tp, CBC, "ProcedureCode"))
      .map(PlainText.unsafe)

    // BT-135: ProcessJustification where listName="direct-award-justification"
    // Prefer the long ProcessReason text; fall back to the short ProcessReasonCode
    val procedureJustification = tpSeqOpt.flatMap { tp =>
      val directAwardPJs = child(tp, CAC, "ProcessJustification")
        .filter { pj =>
          child(NodeSeq.fromSeq(Seq(pj)), CBC, "ProcessReasonCode")
            .headOption.map(_ \@ "listName").contains("direct-award-justification")
        }
      val pj = directAwardPJs.headOption
      pj.flatMap { node =>
        val pjSeq = NodeSeq.fromSeq(Seq(node))
        opt(pjSeq, CBC, "ProcessReason")            // long text (BT-135 description)
          .orElse(opt(pjSeq, CBC, "ProcessReasonCode")) // fallback to short code
      }.map(PlainText.unsafe)
    }

    for
      metadata  <- parseNoticeMetadata(root)
      buyers    <- collectAll(buyersE)
      orgs      <- collectAll(orgsE)
      lots      <- collectAll(lotsE)
      groups    <- collectAll(groupsE)
      parts     <- collectAll(partsE)
      nResult   <- parseNoticeResult(ext)
    yield
      Notice(metadata, buyers, orgs, lots, groups, parts, nResult,
             procedureScope, procedureCode, procedureJustification)

  // ── Public entry point ────────────────────────────────────────────────────────

  def parse(file: java.io.File): Either[ParseError, NoticeForm] =
    Try(XML.loadFile(file)).toEither
      .left.map(e => ParseError.XmlLoadError(file.getPath, e))
      .flatMap { root =>
        parseNotice(root).map { notice =>
          val subtype = notice.metadata.noticeTypeCode
          root.label match
            case "PriorInformationNotice" => PinNotice(subtype, notice)
            case "ContractNotice"         => CnNotice(subtype, notice)
            case "ContractAwardNotice"    => CanNotice(subtype, notice)
            case other                    => UnknownNotice(other, notice)
        }
      }
