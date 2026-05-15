package io.github.khanr1.tedscraper
package r209
package parser

import scala.xml.*
import scala.util.Try
import java.io.File

import io.github.khanr1.tedscraper.common.types.*
import io.github.khanr1.tedscraper.common.{
  TechnicalSection, LinksSection, Link, Sender, SenderUser, Login,
  CodedDataSection, RefOjs, OjNumber, NoticeData, UriDoc, CpvEntry,
  NutsEntry, RefNotice, CodifData, AuthorityTypeField, DocumentTypeField,
  ContractNatureField, ProcedureField, RegulationField, TypeBidField,
  AwardCritField, MainActivityField, HeadingCode, TranslationSection,
  TitleTranslation, AuthorityNameTranslation, TransliteratedAddress,
  FormMeta, RichText
}

import r209.*

/** XML parser for R2.0.9 (2014-directive) TED notices. */
object XmlParser:

  // ── Low-level helpers ───────────────────────────────────────────────────────

  private def txt(n: NodeSeq, names: String*): String =
    names.foldLeft(n)(_ \ _).headOption
      .map(_.text.trim.replaceAll("\\s+", " "))
      .getOrElse("")

  private def opt(n: NodeSeq, names: String*): Option[String] =
    val v = txt(n, names*)
    if v.isEmpty then None else Some(v)

  private def atr(n: NodeSeq, attrName: String): String =
    n.headOption.map(_ \@ attrName).getOrElse("")

  /** Extract NUTS codes from any namespace variant (none, n2016:, n2021:). */
  private def nutsFromNode(n: NodeSeq): List[NutsCode] =
    (n \\ "_")
      .filter(_.label == "NUTS")
      .map(_ \@ "CODE")
      .filter(_.nonEmpty)
      .map(NutsCode.unsafe)
      .toList

  /** Parse VAL_TOTAL / VAL_ESTIMATED_TOTAL / VAL_OBJECT: text content + @CURRENCY. */
  private def valCurrency(
      n: NodeSeq,
      name: String
  ): (Option[MonetaryAmount], Option[Currency]) =
    (n \ name).headOption match
      case None => (None, None)
      case Some(el) =>
        val amt = MonetaryAmount.fromString(el.text.trim).toOption
        val cur = Option(el \@ "CURRENCY").filter(_.nonEmpty).map(Currency.from)
        (amt, cur)

  /** Parse VAL_RANGE_TOTAL: @LOW + @HIGH + @CURRENCY. */
  private def valRange(
      n: NodeSeq
  ): (Option[MonetaryAmount], Option[MonetaryAmount], Option[Currency]) =
    (n \ "VAL_RANGE_TOTAL").headOption match
      case None => (None, None, None)
      case Some(el) =>
        val lo  = MonetaryAmount.fromString(el \@ "LOW").toOption
        val hi  = MonetaryAmount.fromString(el \@ "HIGH").toOption
        val cur = Option(el \@ "CURRENCY").filter(_.nonEmpty).map(Currency.from)
        (lo, hi, cur)

  /** ISO date as plain text (YYYY-MM-DD or YYYYMMDD). */
  private def isoDate(n: NodeSeq, name: String): Option[String] =
    opt(n, name)

  // ── Address parsing ─────────────────────────────────────────────────────────

  /** Parse ADDRESS_CONTRACTING_BODY / ADDRESS_CONTRACTOR / ADDRESS_REVIEW_BODY. */
  private def parseAddress(n: NodeSeq): AddressContractingBody =
    AddressContractingBody(
      officialName = opt(n, "OFFICIALNAME").map(OfficialName(_)),
      nationalId   = opt(n, "NATIONALID").map(RichText(_)),
      address      = opt(n, "ADDRESS").map(StreetAddress(_)),
      town         = opt(n, "TOWN").map(TownName(_)),
      postalCode   = opt(n, "POSTAL_CODE").map(PostalCode(_)),
      country      = {
        val v = atr(n \ "COUNTRY", "VALUE")
        if v.nonEmpty then Some(CountryCode.toDomain(v)) else None
      },
      nuts         = nutsFromNode(n),
      contactPoint = opt(n, "CONTACT_POINT").map(RichText(_)),
      phone        = opt(n, "PHONE").map(PhoneNumber(_)),
      fax          = opt(n, "FAX").map(FaxNumber(_)),
      email        = opt(n, "E_MAIL").map(EmailAddress(_)),
      urlGeneral   = opt(n, "URL_GENERAL").map(Url(_)),
      urlBuyer     = opt(n, "URL_BUYER").map(Url(_)),
      url          = opt(n, "URL").map(Url(_))
    )

  // ── ContractingBody ─────────────────────────────────────────────────────────

  private def parseContractingBody(cb: NodeSeq): ContractingBody =
    val addr = parseAddress(cb \ "ADDRESS_CONTRACTING_BODY")
    val additional = (cb \ "ADDRESS_CONTRACTING_BODY_ADDITIONAL")
      .map(a => parseAddress(NodeSeq.fromSeq(Seq(a))))
      .toList
    ContractingBody(
      address           = addr,
      additionalAddresses = additional,
      jointProcurement  = (cb \ "JOINT_PROCUREMENT_INVOLVED").nonEmpty,
      centralPurchasing = (cb \ "CENTRAL_PURCHASING").nonEmpty,
      authorityType     = {
        val v = atr(cb \ "CA_TYPE", "VALUE")
        if v.nonEmpty then Some(v)
        else opt(cb, "CA_TYPE_OTHER")
      },
      authorityActivity = {
        val v = atr(cb \ "CA_ACTIVITY", "VALUE")
        if v.nonEmpty then Some(v)
        else opt(cb, "CA_ACTIVITY_OTHER")
      },
      entityActivity    = {
        val v = atr(cb \ "CE_ACTIVITY", "VALUE")
        if v.nonEmpty then Some(v)
        else opt(cb, "CE_ACTIVITY_OTHER")
      },
      documentAccess    =
        if (cb \ "DOCUMENT_FULL").nonEmpty then Some(true)
        else if (cb \ "DOCUMENT_RESTRICTED").nonEmpty then Some(false)
        else None,
      urlDocument       = opt(cb, "URL_DOCUMENT").map(Url(_))
    )

  // ── AwardCriteria ───────────────────────────────────────────────────────────

  private def parseAwardCriteria(n: NodeSeq): Option[AwardCriteria] =
    val hasAcBlock = (n \\ "AC_PRICE").nonEmpty || (n \\ "AC_QUALITY").nonEmpty ||
                     (n \\ "AC_COST").nonEmpty  || (n \\ "AC_PROCUREMENT_DOC").nonEmpty
    if !hasAcBlock then None
    else
      val quality = (n \\ "AC_QUALITY").map { q =>
        AwardCriterion(
          RichText(txt(NodeSeq.fromSeq(Seq(q)), "AC_CRITERION")),
          opt(NodeSeq.fromSeq(Seq(q)), "AC_WEIGHTING").map(RichText(_))
        )
      }.toList
      val cost = (n \\ "AC_COST").map { c =>
        AwardCriterion(
          RichText(txt(NodeSeq.fromSeq(Seq(c)), "AC_CRITERION")),
          opt(NodeSeq.fromSeq(Seq(c)), "AC_WEIGHTING").map(RichText(_))
        )
      }.toList
      Some(AwardCriteria(
        lowestPrice      = (n \\ "AC_PRICE").nonEmpty && quality.isEmpty && cost.isEmpty,
        qualityCriteria  = quality,
        costCriteria     = cost
      ))

  // ── ObjectDescr ─────────────────────────────────────────────────────────────

  private def parseObjectDescr(el: Node): ObjectDescr =
    val n = NodeSeq.fromSeq(Seq(el))
    val item = Try((el \@ "ITEM").toInt).getOrElse(1)
    val (durType, durVal) = (n \ "DURATION").headOption match
      case None    => (None, None)
      case Some(d) => (Option(d \@ "TYPE").filter(_.nonEmpty), Try(d.text.trim.toInt).toOption)
    val (valObj, valCur) = valCurrency(n, "VAL_OBJECT")
    ObjectDescr(
      item             = item,
      title            = opt(n, "TITLE").orElse(opt(n, "TITLE", "P")).map(RichText(_)),
      lotNo            = opt(n, "LOT_NO").map(RichText(_)),
      cpvAdditional    = (n \ "CPV_ADDITIONAL" \ "CPV_CODE")
                           .map(e => CpvCode.unsafe(e \@ "CODE"))
                           .filter(_.value.nonEmpty)
                           .toList,
      nuts             = nutsFromNode(n),
      mainSite         = opt(n, "MAIN_SITE").orElse(opt(n, "MAIN_SITE", "P")).map(RichText(_)),
      shortDescr       = opt(n, "SHORT_DESCR").orElse(opt(n, "SHORT_DESCR", "P")).map(RichText(_)),
      awardCriteria    = parseAwardCriteria(n),
      valObject        = valObj,
      valObjectCurrency = valCur,
      durationType     = durType,
      durationValue    = durVal,
      dateStart        = isoDate(n, "DATE_START"),
      dateEnd          = isoDate(n, "DATE_END"),
      renewal          = if (n \ "RENEWAL").nonEmpty then Some(true)
                         else if (n \ "NO_RENEWAL").nonEmpty then Some(false)
                         else None,
      renewalDescr     = opt(n, "RENEWAL_DESCR").map(RichText(_)),
      acceptedVariants = if (n \ "ACCEPTED_VARIANTS").nonEmpty then Some(true)
                         else if (n \ "NO_ACCEPTED_VARIANTS").nonEmpty then Some(false)
                         else None,
      options          = if (n \ "OPTIONS").nonEmpty then Some(true)
                         else if (n \ "NO_OPTIONS").nonEmpty then Some(false)
                         else None,
      optionsDescr     = opt(n, "OPTIONS_DESCR").map(RichText(_)),
      euProject        = if (n \ "EU_PROGR_RELATED").nonEmpty
                         then opt(n, "EU_PROGR_RELATED").orElse(opt(n, "EU_PROGR_RELATED", "P")).map(RichText(_))
                         else None,
      infoAdd          = opt(n, "INFO_ADD").orElse(opt(n, "INFO_ADD", "P")).map(RichText(_))
    )

  // ── ObjectContract ──────────────────────────────────────────────────────────

  private def parseObjectContract(oc: NodeSeq): ObjectContract =
    val title = opt(oc, "TITLE", "P")
      .orElse(opt(oc, "TITLE"))
      .getOrElse("")
    val cpvCode = CpvCode.unsafe(atr(oc \ "CPV_MAIN" \ "CPV_CODE", "CODE"))
    val (estVal, estCur) = valCurrency(oc, "VAL_ESTIMATED_TOTAL")
    val (totVal, totCur) = valCurrency(oc, "VAL_TOTAL")
    val lotDivision = (oc \ "LOT_DIVISION").nonEmpty
    val lots = (oc \ "OBJECT_DESCR").map(parseObjectDescr).toList
    ObjectContract(
      title              = RichText(title),
      referenceNumber    = opt(oc, "REFERENCE_NUMBER").map(RichText(_)),
      cpvMain            = cpvCode,
      contractType       = {
        val v = atr(oc \ "TYPE_CONTRACT", "CTYPE")
        if v.nonEmpty then Some(v) else None
      },
      shortDescr         = opt(oc, "SHORT_DESCR", "P")
                             .orElse(opt(oc, "SHORT_DESCR"))
                             .map(RichText(_)),
      valEstimatedTotal  = estVal,
      valEstimatedCurrency = estCur,
      valTotal           = totVal,
      valTotalCurrency   = totCur,
      lotDivision        = lotDivision,
      lots               = lots
    )

  // ── Procedure ───────────────────────────────────────────────────────────────

  private def parseProcedure(proc: NodeSeq): Procedure =
    // Procedure type: detect by element presence (including inside DIRECTIVE wrappers)
    val procType: Option[ProcedureType2014] =
      if (proc \\ "PT_OPEN").nonEmpty                           then Some(ProcedureType2014.Open)
      else if (proc \\ "PT_RESTRICTED").nonEmpty                then Some(ProcedureType2014.Restricted)
      else if (proc \\ "PT_COMPETITIVE_NEGOTIATION").nonEmpty   then Some(ProcedureType2014.CompetitiveNegotiation)
      else if (proc \\ "PT_COMPETITIVE_DIALOGUE").nonEmpty      then Some(ProcedureType2014.CompetitiveDialogue)
      else if (proc \\ "PT_INNOVATION_PARTNERSHIP").nonEmpty    then Some(ProcedureType2014.InnovationPartnership)
      else if (proc \\ "PT_NEGOTIATED_WITH_PRIOR_CALL").nonEmpty then Some(ProcedureType2014.NegotiatedWithPriorCall)
      else if (proc \\ "PT_AWARD_CONTRACT_WITHOUT_CALL").nonEmpty then Some(ProcedureType2014.AwardWithoutCall)
      else if (proc \\ "PT_AWARD_CONTRACT_WITHOUT_PUBLICATION").nonEmpty then Some(ProcedureType2014.AwardWithoutPublication)
      else None

    // Justification may be inside PT_AWARD_CONTRACT_WITHOUT_CALL > D_JUSTIFICATION
    val justification = (proc \\ "D_JUSTIFICATION").headOption
      .flatMap(j => {
        val t = j.text.trim.replaceAll("\\s+", " ")
        if t.nonEmpty then Some(RichText(t)) else None
      })

    Procedure(
      procedureType    = procType,
      accelerated      = opt(proc, "ACCELERATED_PROC").map(RichText(_)),
      framework        = (proc \ "FRAMEWORK").nonEmpty,
      dps              = (proc \ "DPS").nonEmpty,
      eauctionUsed     = (proc \ "EAUCTION_USED").nonEmpty,
      gpa              = if (proc \\ "CONTRACT_COVERED_GPA").nonEmpty then Some(true)
                         else if (proc \\ "NO_CONTRACT_COVERED_GPA").nonEmpty then Some(false)
                         else None,
      noticeNumberOj   = opt(proc, "NOTICE_NUMBER_OJ"),
      justification    = justification,
      dateReceiptTenders = isoDate(proc, "DATE_RECEIPT_TENDERS"),
      timeReceiptTenders = opt(proc, "TIME_RECEIPT_TENDERS"),
      languages        = (proc \ "LANGUAGES" \ "LANGUAGE")
                           .map(_ \@ "VALUE")
                           .filter(_.nonEmpty)
                           .toList,
      dateAwardScheduled = isoDate(proc, "DATE_AWARD_SCHEDULED")
    )

  private val emptyProcedure: Procedure =
    Procedure(None, None, false, false, false, None, None, None, None, None, Nil, None)

  // ── Lefti ───────────────────────────────────────────────────────────────────

  private def parseLefti(lefti: NodeSeq): Lefti =
    Lefti(
      suitability          = opt(lefti, "SUITABILITY").orElse(opt(lefti, "SUITABILITY", "P")).map(RichText(_)),
      economicCriteriaDoc  = if (lefti \ "ECONOMIC_CRITERIA_DOC").nonEmpty then Some(true) else None,
      economicFinancialInfo = opt(lefti, "ECONOMIC_FINANCIAL_INFO").map(RichText(_)),
      economicMinLevel     = opt(lefti, "ECONOMIC_FINANCIAL_MIN_LEVEL").map(RichText(_)),
      technicalCriteriaDoc = if (lefti \ "TECHNICAL_CRITERIA_DOC").nonEmpty then Some(true) else None,
      technicalProfessionalInfo = opt(lefti, "TECHNICAL_PROFESSIONAL_INFO").map(RichText(_)),
      technicalMinLevel    = opt(lefti, "TECHNICAL_MINIMUM_LEVEL").map(RichText(_)),
      reservedSheltered    = if (lefti \ "RESTRICTED_SHELTERED_WORKSHOP").nonEmpty then Some(true) else None,
      performanceConditions = opt(lefti, "PERFORMANCE_CONDITIONS").map(RichText(_)),
      staffQualification   = if (lefti \ "PERFORMANCE_STAFF_QUALIFICATION").nonEmpty then Some(true) else None
    )

  // ── ComplementaryInfo ───────────────────────────────────────────────────────

  private def parseComplementaryInfo(ci: NodeSeq): ComplementaryInfo =
    val rb = ci \ "ADDRESS_REVIEW_BODY"
    ComplementaryInfo(
      recurrentProcurement = if (ci \ "RECURRENT_PROCUREMENT").nonEmpty then Some(true)
                             else if (ci \ "NO_RECURRENT_PROCUREMENT").nonEmpty then Some(false)
                             else None,
      estimatedTiming      = opt(ci, "ESTIMATED_TIMING").map(RichText(_)),
      eOrdering            = (ci \ "EORDERING").nonEmpty,
      eInvoicing           = (ci \ "EINVOICING").nonEmpty,
      ePayment             = (ci \ "EPAYMENT").nonEmpty,
      infoAdd              = opt(ci, "INFO_ADD", "P")
                               .orElse(opt(ci, "INFO_ADD"))
                               .map(RichText(_)),
      reviewBodyName       = opt(rb, "OFFICIALNAME").map(OfficialName(_)),
      reviewBodyCountry    = {
        val v = atr(rb \ "COUNTRY", "VALUE")
        if v.nonEmpty then Some(CountryCode.toDomain(v)) else None
      },
      dateDispatch         = isoDate(ci, "DATE_DISPATCH_NOTICE")
    )

  private val emptyCI: ComplementaryInfo =
    ComplementaryInfo(None, None, false, false, false, None, None, None, None)

  // ── Contractor + AwardContractItem ──────────────────────────────────────────

  private def parseContractor(el: Node): Contractor =
    val addrEl = (NodeSeq.fromSeq(Seq(el)) \ "ADDRESS_CONTRACTOR").headOption
      .map(NodeSeq.fromSeq)
      .getOrElse(NodeSeq.fromSeq(Seq(el))) // fallback: treat el itself as address
    val sme = el.label match
      case _ =>
        val n = NodeSeq.fromSeq(Seq(el))
        if (n \ "SME").nonEmpty then Some(true)
        else if (n \ "NO_SME").nonEmpty then Some(false)
        else None
    Contractor(address = parseAddress(addrEl), isSme = sme)

  private def parseAwardContractItem(el: Node): AwardContractItem =
    val n    = NodeSeq.fromSeq(Seq(el))
    val item = Try((el \@ "ITEM").toInt).getOrElse(1)
    val noAward = (n \ "NO_AWARDED_CONTRACT").nonEmpty
    if noAward then
      AwardContractItem(
        item        = item,
        contractNo  = opt(n, "CONTRACT_NO").map(RichText(_)),
        lotNo       = opt(n, "LOT_NO").map(RichText(_)),
        title       = opt(n, "TITLE", "P").orElse(opt(n, "TITLE")).map(RichText(_)),
        noAward     = true,
        awarded     = None
      )
    else
      val ac = (n \ "AWARDED_CONTRACT").headOption.map(NodeSeq.fromSeq).getOrElse(n)
      // Tenders — inside TENDERS wrapper or bare NB_TENDERS_RECEIVED
      val tendersReceived    = (ac \\ "NB_TENDERS_RECEIVED").headOption.flatMap(e => Try(e.text.trim.toInt).toOption)
      val tendersRecSme      = (ac \\ "NB_TENDERS_RECEIVED_SME").headOption.flatMap(e => Try(e.text.trim.toInt).toOption)
      val tendersRecOtherEu  = (ac \\ "NB_TENDERS_RECEIVED_OTHER_EU").headOption.flatMap(e => Try(e.text.trim.toInt).toOption)
      val tendersRecNonEu    = (ac \\ "NB_TENDERS_RECEIVED_NON_EU").headOption.flatMap(e => Try(e.text.trim.toInt).toOption)
      // Contractors — inside CONTRACTORS wrapper or bare
      val awardedToGroup = (ac \\ "AWARDED_TO_GROUP").nonEmpty
      val contractors = (ac \\ "CONTRACTOR").map(parseContractor).toList
      // Values — inside VALUES wrapper or bare VAL_TOTAL
      val valTotalEl = (ac \\ "VAL_TOTAL").headOption
      val valTotal   = valTotalEl.flatMap(e => MonetaryAmount.fromString(e.text.trim).toOption)
      val valTotCur  = valTotalEl.flatMap(e => Option(e \@ "CURRENCY").filter(_.nonEmpty).map(Currency.from))
      val valEstEl   = (ac \\ "VAL_ESTIMATED_TOTAL").headOption
      val valEst     = valEstEl.flatMap(e => MonetaryAmount.fromString(e.text.trim).toOption)
      val valEstCur  = valEstEl.flatMap(e => Option(e \@ "CURRENCY").filter(_.nonEmpty).map(Currency.from))
      val (rangeLo, rangeHi, rangeCur) = valRange(ac)
      AwardContractItem(
        item        = item,
        contractNo  = opt(n, "CONTRACT_NO").map(RichText(_)),
        lotNo       = opt(n, "LOT_NO").map(RichText(_)),
        title       = opt(n, "TITLE", "P").orElse(opt(n, "TITLE")).map(RichText(_)),
        noAward     = false,
        awarded     = Some(AwardedContract(
          dateConclusion     = isoDate(ac, "DATE_CONCLUSION_CONTRACT"),
          tendersReceived    = tendersReceived,
          tendersReceivedSme = tendersRecSme,
          tendersReceivedOtherEu = tendersRecOtherEu,
          tendersReceivedNonEu   = tendersRecNonEu,
          awardedToGroup     = awardedToGroup,
          contractors        = contractors,
          valEstimated       = valEst,
          valEstimatedCurrency = valEstCur,
          valTotal           = valTotal,
          valTotalCurrency   = valTotCur,
          valRangeLow        = rangeLo,
          valRangeHigh       = rangeHi,
          valRangeCurrency   = rangeCur,
          subcontracted      = if (ac \\ "LIKELY_SUBCONTRACTED").nonEmpty then Some(true) else None
        ))
      )

  // ── Change (F14) ────────────────────────────────────────────────────────────

  private def parseChange(el: Node): Change =
    val n = NodeSeq.fromSeq(Seq(el))
    val where = n \ "WHERE"
    Change(
      section  = opt(where, "SECTION"),
      lotNo    = opt(where, "LOT_NO"),
      label    = opt(where, "LABEL"),
      oldValue = opt(n, "OLD_VALUE", "TEXT", "P").orElse(opt(n, "OLD_VALUE")),
      newValue = opt(n, "NEW_VALUE", "TEXT", "P").orElse(opt(n, "NEW_VALUE"))
    )

  // ── Legal basis extraction ──────────────────────────────────────────────────

  private def legalBasis(form: NodeSeq): Option[String] =
    (form \ "LEGAL_BASIS").headOption.map(_ \@ "VALUE").filter(_.nonEmpty)
      .orElse((form \ "DIRECTIVE").headOption.map(_ \@ "VALUE").filter(_.nonEmpty))

  // ── FormMeta ────────────────────────────────────────────────────────────────

  private def parseFormMeta(formEl: Node): FormMeta =
    FormMeta(
      language     = Language.from(formEl \@ "LG"),
      category     = FormCategory.fromString(formEl \@ "CATEGORY"),
      version      = Option(formEl \@ "VERSION").filter(_.nonEmpty).map(RichText(_)),
      contractType = None
    )

  // ── Form parsers ─────────────────────────────────────────────────────────────

  private def parseF01(form: NodeSeq, meta: FormMeta): F01PriorInfo2014 =
    F01PriorInfo2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      noticeType       = (form \ "NOTICE").headOption.map(_ \@ "TYPE").filter(_.nonEmpty),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContracts  = (form \ "OBJECT_CONTRACT").map(oc => parseObjectContract(NodeSeq.fromSeq(Seq(oc)))).toList,
      lefti            = if (form \ "LEFTI").nonEmpty then Some(parseLefti(form \ "LEFTI")) else None,
      procedure        = if (form \ "PROCEDURE").nonEmpty then parseProcedure(form \ "PROCEDURE") else emptyProcedure,
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseF02(form: NodeSeq, meta: FormMeta): F02ContractNotice2014 =
    F02ContractNotice2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContract   = parseObjectContract(form \ "OBJECT_CONTRACT"),
      lefti            = if (form \ "LEFTI").nonEmpty then Some(parseLefti(form \ "LEFTI")) else None,
      procedure        = if (form \ "PROCEDURE").nonEmpty then parseProcedure(form \ "PROCEDURE") else emptyProcedure,
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseF03(form: NodeSeq, meta: FormMeta): F03ContractAward2014 =
    F03ContractAward2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContract   = parseObjectContract(form \ "OBJECT_CONTRACT"),
      procedure        = if (form \ "PROCEDURE").nonEmpty then parseProcedure(form \ "PROCEDURE") else emptyProcedure,
      awards           = (form \ "AWARD_CONTRACT").map(parseAwardContractItem).toList,
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseF04(form: NodeSeq, meta: FormMeta): F04PeriodicIndicative2014 =
    F04PeriodicIndicative2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContracts  = (form \ "OBJECT_CONTRACT").map(oc => parseObjectContract(NodeSeq.fromSeq(Seq(oc)))).toList,
      lefti            = if (form \ "LEFTI").nonEmpty then Some(parseLefti(form \ "LEFTI")) else None,
      procedure        = if (form \ "PROCEDURE").nonEmpty then parseProcedure(form \ "PROCEDURE") else emptyProcedure,
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseF05(form: NodeSeq, meta: FormMeta): F05ContractNoticeUtilities2014 =
    F05ContractNoticeUtilities2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContract   = parseObjectContract(form \ "OBJECT_CONTRACT"),
      lefti            = if (form \ "LEFTI").nonEmpty then Some(parseLefti(form \ "LEFTI")) else None,
      procedure        = if (form \ "PROCEDURE").nonEmpty then parseProcedure(form \ "PROCEDURE") else emptyProcedure,
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseF06(form: NodeSeq, meta: FormMeta): F06ContractAwardUtilities2014 =
    F06ContractAwardUtilities2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContract   = parseObjectContract(form \ "OBJECT_CONTRACT"),
      procedure        = if (form \ "PROCEDURE").nonEmpty then parseProcedure(form \ "PROCEDURE") else emptyProcedure,
      awards           = (form \ "AWARD_CONTRACT").map(parseAwardContractItem).toList,
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseF08(form: NodeSeq, meta: FormMeta): F08BuyerProfile2014 =
    F08BuyerProfile2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContract   = parseObjectContract(form \ "OBJECT_CONTRACT"),
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseF12(form: NodeSeq, meta: FormMeta): F12DesignContest2014 =
    F12DesignContest2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContract   = parseObjectContract(form \ "OBJECT_CONTRACT"),
      lefti            = if (form \ "LEFTI").nonEmpty then Some(parseLefti(form \ "LEFTI")) else None,
      procedure        = if (form \ "PROCEDURE").nonEmpty then parseProcedure(form \ "PROCEDURE") else emptyProcedure,
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseF13(form: NodeSeq, meta: FormMeta): F13DesignContestResult2014 =
    F13DesignContestResult2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContract   = parseObjectContract(form \ "OBJECT_CONTRACT"),
      procedure        = if (form \ "PROCEDURE").nonEmpty then parseProcedure(form \ "PROCEDURE") else emptyProcedure,
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseF14(form: NodeSeq, meta: FormMeta): F14Corrigendum2014 =
    // F14 has no LEGAL_BASIS in older files; OBJECT_CONTRACT is minimal (no lots)
    // NOTICE_NUMBER_OJ is in COMPLEMENTARY_INFO (not PROCEDURE)
    val oc = (form \ "OBJECT_CONTRACT").headOption.map { ocEl =>
      val n = NodeSeq.fromSeq(Seq(ocEl))
      ObjectContract(
        title              = RichText(opt(n, "TITLE", "P").orElse(opt(n, "TITLE")).getOrElse("")),
        referenceNumber    = opt(n, "REFERENCE_NUMBER").map(RichText(_)),
        cpvMain            = CpvCode.unsafe(atr(n \ "CPV_MAIN" \ "CPV_CODE", "CODE")),
        contractType       = { val v = atr(n \ "TYPE_CONTRACT", "CTYPE"); if v.nonEmpty then Some(v) else None },
        shortDescr         = opt(n, "SHORT_DESCR", "P").orElse(opt(n, "SHORT_DESCR")).map(RichText(_)),
        valEstimatedTotal  = None, valEstimatedCurrency = None,
        valTotal           = None, valTotalCurrency = None,
        lotDivision        = false,
        lots               = Nil
      )
    }.getOrElse(ObjectContract(RichText(""), None, CpvCode.unsafe(""), None, None, None, None, None, None, false, Nil))

    F14Corrigendum2014(
      meta              = meta,
      legalBasis        = legalBasis(form),
      contractingBody   = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContract    = oc,
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO"),
      changes           = (form \ "CHANGES" \ "CHANGE").map(parseChange).toList
    )

  private def parseF15(form: NodeSeq, meta: FormMeta): F15Veat2014 =
    F15Veat2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContract   = parseObjectContract(form \ "OBJECT_CONTRACT"),
      procedure        = if (form \ "PROCEDURE").nonEmpty then parseProcedure(form \ "PROCEDURE") else emptyProcedure,
      awards           = (form \ "AWARD_CONTRACT").map(parseAwardContractItem).toList,
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseF20(form: NodeSeq, meta: FormMeta): F20Modification2014 =
    F20Modification2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContract   = parseObjectContract(form \ "OBJECT_CONTRACT"),
      procedure        = if (form \ "PROCEDURE").nonEmpty then Some(parseProcedure(form \ "PROCEDURE")) else None,
      award            = (form \ "AWARD_CONTRACT").headOption.map(parseAwardContractItem),
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseF21(form: NodeSeq, meta: FormMeta): F21SocialServices2014 =
    F21SocialServices2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContracts  = (form \ "OBJECT_CONTRACT").map(oc => parseObjectContract(NodeSeq.fromSeq(Seq(oc)))).toList,
      lefti            = if (form \ "LEFTI").nonEmpty then Some(parseLefti(form \ "LEFTI")) else None,
      procedure        = if (form \ "PROCEDURE").nonEmpty then Some(parseProcedure(form \ "PROCEDURE")) else None,
      awards           = (form \ "AWARD_CONTRACT").map(parseAwardContractItem).toList,
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseF24(form: NodeSeq, meta: FormMeta): F24ConcessionNotice2014 =
    F24ConcessionNotice2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContract   = parseObjectContract(form \ "OBJECT_CONTRACT"),
      lefti            = if (form \ "LEFTI").nonEmpty then Some(parseLefti(form \ "LEFTI")) else None,
      procedure        = if (form \ "PROCEDURE").nonEmpty then parseProcedure(form \ "PROCEDURE") else emptyProcedure,
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseF25(form: NodeSeq, meta: FormMeta): F25ConcessionAward2014 =
    F25ConcessionAward2014(
      meta             = meta,
      legalBasis       = legalBasis(form),
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContract   = parseObjectContract(form \ "OBJECT_CONTRACT"),
      procedure        = if (form \ "PROCEDURE").nonEmpty then parseProcedure(form \ "PROCEDURE") else emptyProcedure,
      awards           = (form \ "AWARD_CONTRACT").map(parseAwardContractItem).toList,
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  private def parseMove(form: NodeSeq, meta: FormMeta): Move2014 =
    Move2014(
      meta             = meta,
      contractingBody  = parseContractingBody(form \ "CONTRACTING_BODY"),
      objectContract   = parseObjectContract(form \ "OBJECT_CONTRACT"),
      complementaryInfo = parseComplementaryInfo(form \ "COMPLEMENTARY_INFO")
    )

  // ── Form dispatch ────────────────────────────────────────────────────────────

  private def parseFormBody(formEl: Node): Either[ParseError, FormBody] =
    val meta = parseFormMeta(formEl)
    val form = NodeSeq.fromSeq(Seq(formEl))
    formEl.label match
      case "F01_2014" => Right(parseF01(form, meta))
      case "F02_2014" => Right(parseF02(form, meta))
      case "F03_2014" => Right(parseF03(form, meta))
      case "F04_2014" => Right(parseF04(form, meta))
      case "F05_2014" => Right(parseF05(form, meta))
      case "F06_2014" => Right(parseF06(form, meta))
      case "F08_2014" => Right(parseF08(form, meta))
      case "F12_2014" => Right(parseF12(form, meta))
      case "F13_2014" => Right(parseF13(form, meta))
      case "F14_2014" => Right(parseF14(form, meta))
      case "F15_2014" => Right(parseF15(form, meta))
      case "F20_2014" => Right(parseF20(form, meta))
      case "F21_2014" => Right(parseF21(form, meta))
      case "F24_2014" => Right(parseF24(form, meta))
      case "F25_2014" => Right(parseF25(form, meta))
      case "MOVE"     => Right(parseMove(form, meta))
      // Skip non-form elements that appear in FORM_SECTION (e.g. NOTICE_UUID)
      case other if !other.contains("_2014") && other != "MOVE" =>
        Left(ParseError.UnknownFormType(other))
      case other => Right(UnknownForm2014(meta, other))

  // ── Envelope section parsers (mirror r208, produce r208 domain types) ────────

  private def parseTechnical(root: Elem): Either[ParseError, TechnicalSection] =
    val tech = root \ "TECHNICAL_SECTION"
    val recId = txt(tech, "RECEPTION_ID")
    val delDate = txt(tech, "DELETION_DATE")
    val lgList = txt(tech, "FORM_LG_LIST")
    if recId.isEmpty then Left(ParseError.MissingField("RECEPTION_ID", "TECHNICAL_SECTION"))
    else if delDate.isEmpty then Left(ParseError.MissingField("DELETION_DATE", "TECHNICAL_SECTION"))
    else if lgList.isEmpty then Left(ParseError.MissingField("FORM_LG_LIST", "TECHNICAL_SECTION"))
    else Right(TechnicalSection(
      receptionId   = ReceptionId.unsafe(recId),
      deletionDate  = TedDate.unsafe(delDate),
      formLanguages = lgList.split("\\s+").map(Language.from).toList,
      comments      = opt(tech, "COMMENTS").map(RichText(_)),
      oldHeading    = opt(tech, "OLD_HEADING").map(HeadingCode(_))
    ))

  private def parseLink(n: NodeSeq, name: String): Option[Link] =
    val el = n \ name
    if el.isEmpty then None
    else Some(Link(
      href     = Url(atr(el, "href")),
      linkType = atr(el, "type"),
      title    = opt(el, "title").map(RichText(_))
    ))

  private def parseLinks(root: Elem): LinksSection =
    val ls = root \ "LINKS_SECTION"
    LinksSection(
      xmlSchemaDefinitionLink = parseLink(ls, "XML_SCHEMA_DEFINITION_LINK"),
      officialFormsLink       = parseLink(ls, "OFFICIAL_FORMS_LINK"),
      formsLabelsLink         = parseLink(ls, "FORMS_LABELS_LINK"),
      originalCpvLink         = parseLink(ls, "ORIGINAL_CPV_LINK"),
      originalNutsLink        = parseLink(ls, "ORIGINAL_NUTS_LINK")
    )

  private def parseSender(root: Elem): Option[Sender] =
    val s = root \ "SENDER"
    if s.isEmpty then None
    else
      val login = Login(
        esenderLogin  = RichText(txt(s, "LOGIN", "ESENDER_LOGIN")),
        customerLogin = opt(s, "LOGIN", "CUSTOMER_LOGIN").map(RichText(_)),
        loginClass    = Option(atr(s \ "LOGIN", "CLASS")).filter(_.nonEmpty).map {
          case "A" => LoginClass.A; case "B" => LoginClass.B
          case "C" => LoginClass.C; case "D" => LoginClass.D
          case _   => LoginClass.Unknown
        }
      )
      Some(Sender(login, None, ExternalDocRef(txt(s, "NO_DOC_EXT"))))

  private def parseCodedDataSection(root: Elem): Either[ParseError, CodedDataSection] =
    val cds  = root \ "CODED_DATA_SECTION"
    val rOjs = cds \ "REF_OJS"
    val noOj = txt(rOjs, "NO_OJ")
    val datePub = txt(rOjs, "DATE_PUB")
    if noOj.isEmpty  then Left(ParseError.MissingField("NO_OJ", "REF_OJS"))
    else if datePub.isEmpty then Left(ParseError.MissingField("DATE_PUB", "REF_OJS"))
    else
      val refOjs = RefOjs(
        number = OjNumber(
          OjIssueNumber.unsafe(noOj),
          OjClass.from(atr(rOjs \ "NO_OJ", "CLASS")),
          atr(rOjs \ "NO_OJ", "LAST") == "YES"
        ),
        publicationDate = TedDate.unsafe(datePub)
      )
      val nd  = cds \ "NOTICE_DATA"
      val noticeData = NoticeData(
        noDocOjs = opt(nd, "NO_DOC_OJS").flatMap(NoticeNumber.from(_).toOption),
        uriList  = {
          val uris = (nd \ "URI_LIST" \ "URI_DOC")
            .map(e => UriDoc(Language.from(e \@ "LG"), IaUrl.unsafe(e.text.trim)))
            .toList
          if uris.isEmpty then None else Some(uris)
        },
        originalLanguage = Language.from(txt(nd, "LG_ORIG")),
        isoCountry       = CountryCode.toDomain(atr(nd \ "ISO_COUNTRY", "VALUE")),
        iaUrlGeneral     = IaUrl.unsafe(txt(nd, "IA_URL_GENERAL")),
        iaUrlEtendering  = opt(nd, "IA_URL_ETENDERING").map(IaUrl.unsafe),
        originalCpvCodes = (nd \ "ORIGINAL_CPV").map { e =>
          CpvEntry(CpvCode.unsafe(e \@ "CODE"), RichText(e.text.trim))
        }.toList,
        currentCpvCodes  = (nd \ "CURRENT_CPV").map { e =>
          CpvEntry(CpvCode.unsafe(e \@ "CODE"), RichText(e.text.trim))
        }.toList,
        originalNutsCodes = (nd \ "ORIGINAL_NUTS").map { e =>
          NutsEntry(NutsCode.unsafe(e \@ "CODE"), RichText(e.text.trim))
        }.toList,
        currentNutsCodes  = Nil,
        valuesList        = None,
        refNotice         = {
          val rn = nd \ "REF_NOTICE"
          if rn.isEmpty then None
          else Some(RefNotice((rn \ "NO_DOC_OJS").flatMap { e =>
            NoticeNumber.from(e.text.trim).toOption
          }.toList))
        }
      )
      val cd = cds \ "CODIF_DATA"
      val dispDate = txt(cd, "DS_DATE_DISPATCH")
      if dispDate.isEmpty then Left(ParseError.MissingField("DS_DATE_DISPATCH", "CODIF_DATA"))
      else
        val codifData = CodifData(
          dispatchDate           = TedDate.unsafe(dispDate),
          documentRequestDate    = opt(cd, "DD_DATE_REQUEST_DOCUMENT").map(TedDateTime.unsafe),
          submissionDate         = opt(cd, "DT_DATE_FOR_SUBMISSION").map(TedDateTime.unsafe),
          authorityType = AuthorityTypeField(
            AuthorityTypeCode.from(atr(cd \ "AA_AUTHORITY_TYPE", "CODE")),
            RichText(txt(cd, "AA_AUTHORITY_TYPE"))
          ),
          documentType = DocumentTypeField(
            DocumentTypeCode.from(atr(cd \ "TD_DOCUMENT_TYPE", "CODE")),
            RichText(txt(cd, "TD_DOCUMENT_TYPE"))
          ),
          contractNature = ContractNatureField(
            ContractNatureCode.from(atr(cd \ "NC_CONTRACT_NATURE", "CODE")),
            RichText(txt(cd, "NC_CONTRACT_NATURE"))
          ),
          procedure = ProcedureField(
            ProcedureCode.from(atr(cd \ "PR_PROC", "CODE")),
            RichText(txt(cd, "PR_PROC"))
          ),
          regulation = RegulationField(
            RegulationCode.from(atr(cd \ "RP_REGULATION", "CODE")),
            RichText(txt(cd, "RP_REGULATION"))
          ),
          typeBid = TypeBidField(
            TypeBidCode.from(atr(cd \ "TY_TYPE_BID", "CODE")),
            RichText(txt(cd, "TY_TYPE_BID"))
          ),
          awardCriteria = AwardCritField(
            AwardCritCode.from(atr(cd \ "AC_AWARD_CRIT", "CODE")),
            RichText(txt(cd, "AC_AWARD_CRIT"))
          ),
          mainActivities = (cd \ "MA_MAIN_ACTIVITIES").map { e =>
            MainActivityField(
              MainActivityCode.from(e \@ "CODE"),
              RichText(e.text.trim)
            )
          }.toList,
          heading   = HeadingCode(txt(cd, "HEADING")),
          directive = opt(cd, "DIRECTIVE")
            .flatMap(_ => Directive.from(atr(cd \ "DIRECTIVE", "VALUE")))
        )
        Right(CodedDataSection(refOjs, noticeData, codifData))

  private def parseTranslationSection(root: Elem): TranslationSection =
    val ts = root \ "TRANSLATION_SECTION"
    TranslationSection(
      titles = (ts \ "ML_TITLES" \ "ML_TI_DOC").map { e =>
        TitleTranslation(
          language = Language.from(e \@ "LG"),
          country  = opt(NodeSeq.fromSeq(Seq(e)), "TI_CY").map(RichText(_)),
          town     = opt(NodeSeq.fromSeq(Seq(e)), "TI_TOWN").map(TownName(_)),
          text     = opt(NodeSeq.fromSeq(Seq(e)), "TI_TEXT").map(RichText(_))
        )
      }.toList,
      authorityNames = (ts \ "ML_AA_NAMES" \ "AA_NAME").map { e =>
        AuthorityNameTranslation(language = Language.from(e \@ "LG"), name = OfficialName(e.text.trim))
      }.toList,
      transliterations = None
    )

  // ── Top-level parse ──────────────────────────────────────────────────────────

  def parse(file: File): Either[ParseError, r209.Notice] =
    Try(XML.loadFile(file)).toEither.left
      .map(e => ParseError.XmlError(file.getName, e))
      .flatMap { root =>
        // Schema version: VERSION attribute on TED_EXPORT, or on form element
        val versionAttr = Option(root \@ "VERSION").filter(_.nonEmpty)
        val formVersionAttr = (root \ "FORM_SECTION").headOption.toList
          .flatMap(_.child.collect { case e: Elem => e })
          .flatMap(e => Option(e \@ "VERSION").filter(_.nonEmpty))
          .headOption
        val schemaVersion = SchemaVersion.from(
          versionAttr
            .orElse(formVersionAttr)
            .getOrElse(root.namespace)
        )

        val docIdStr = root \@ "DOC_ID"
        val editionStr = root \@ "EDITION"

        for
          docId    <- DocumentId.from(docIdStr).left.map(e => ParseError.MissingField("DOC_ID", e))
          edition  <- Edition.from(editionStr).left.map(e => ParseError.MissingField("EDITION", e))
          tech     <- parseTechnical(root)
          coded    <- parseCodedDataSection(root)
          formSect <- {
            val formElems = (root \ "FORM_SECTION").headOption.toList
              .flatMap(_.child.collect { case e: Elem => e })
            val results = formElems.map(parseFormBody)
            // UnknownFormType errors (e.g. NOTICE_UUID) are silently skipped;
            // only hard errors (XmlError, MissingField) fail the notice.
            val hardErrors = results.collect {
              case Left(e: ParseError.XmlError)    => e
              case Left(e: ParseError.MissingField) => e
            }
            if hardErrors.nonEmpty then Left(hardErrors.head)
            else Right(FormSection(results.collect { case Right(fb) => fb }))
          }
        yield r209.Notice(
          docId             = docId,
          edition           = edition,
          schemaVersion     = schemaVersion,
          technicalSection  = tech,
          linkSection       = parseLinks(root),
          sender            = parseSender(root),
          codedDataSection  = coded,
          translationSection = parseTranslationSection(root),
          formSection       = formSect
        )
      }
