package io.github.khanr1.tedscraper
package r208
package parser

import types.*

import scala.xml.*
import scala.util.Try
import java.io.File

/** Low-level XML parsing utilities for R208 forms. */
object XmlParser:

  // ── Low-level XML helpers ───────────────────────────────────────────────────

  /** Text of first element reached by walking the local-name path. Empty string
    * if absent.
    */
  private def txt(n: NodeSeq, names: String*): String =
    names.foldLeft(n)(_ \ _).headOption
      .map(_.text.trim.replaceAll("\\s+", " "))
      .getOrElse("")

  /** Same but returns None when result is empty. */
  private def opt(n: NodeSeq, names: String*): Option[String] =
    val v = txt(n, names*)
    if v.isEmpty then None else Some(v)

  /** Attribute on first element matching the path. */
  private def atr(n: NodeSeq, attrName: String, names: String*): String =
    names.foldLeft(n)(_ \ _).headOption.map(_ \@ attrName).getOrElse("")

  /** Deep-search for all elements with given local name. */
  private def deep(n: NodeSeq, name: String): NodeSeq = n \\ name

  /** Require a non-empty String, lifting it to Either. */
  private def req(
      value: String,
      field: String,
      ctx: String
  ): Either[ParseError, String] =
    if value.nonEmpty then Right(value)
    else Left(ParseError.MissingField(field, ctx))

  /** Build a StructuredDate from a node whose children are DAY / MONTH / YEAR.
    * Returns None when any field is absent (date is optional in most contexts).
    */
  private def structDate(n: NodeSeq): Option[StructuredDate] =
    if n.isEmpty then None
    else
      val y = txt(n, "YEAR"); val m = txt(n, "MONTH"); val d = txt(n, "DAY")
      if y.nonEmpty && m.nonEmpty && d.nonEmpty
      then Some(StructuredDate(RichText(y), RichText(m), RichText(d)))
      else None

  /** Build a StructuredDate by first descending to a named child. */
  private def structDateAt(n: NodeSeq, name: String): Option[StructuredDate] =
    structDate(
      (n \\ name).headOption.map(NodeSeq.fromSeq).getOrElse(NodeSeq.Empty)
    )

  // ─────────────────────────────────────────────────────────────────────────────
  //  Shared sub-parsers (used by multiple form types)
  // ─────────────────────────────────────────────────────────────────────────────

  /** Parse a CA_CE_CONCESSIONAIRE_PROFILE element. ORGANISATION, ADDRESS, TOWN,
    * COUNTRY are required by the schema; we use empty wrappers rather than
    * failing so bulk parsing remains resilient.
    */
  private def parseContractingProfile(n: NodeSeq): ContractingProfile =
    val profile = (n \\ "CA_CE_CONCESSIONAIRE_PROFILE").headOption
      .map(NodeSeq.fromSeq)
      .getOrElse(NodeSeq.Empty)
    ContractingProfile(
      officialName = OfficialName(txt(profile, "ORGANISATION", "OFFICIALNAME")),
      address = StreetAddress(txt(profile, "ADDRESS")),
      town = TownName(txt(profile, "TOWN")),
      postalCode = opt(profile, "POSTAL_CODE").map(PostalCode(_)),
      country = CountryCode.toDomain(atr(profile \ "COUNTRY", "VALUE")),
      contactPoint = opt(profile, "CONTACT_POINT").map(RichText(_)),
      attention = opt(profile, "ATTENTION").map(RichText(_)),
      phone = opt(profile, "PHONE").map(PhoneNumber(_)),
      emails = (profile \ "E_MAILS" \ "E_MAIL")
        .map(e => EmailAddress(e.text.trim))
        .toList,
      fax = opt(profile, "FAX").map(FaxNumber(_))
    )

  /** Parse a CONTACT_DATA_WITHOUT_RESPONSIBLE_NAME element (INC_05 group). All
    * fields optional.
    */
  private def parseContactData(n: NodeSeq): ContactData =
    val cd: NodeSeq =
      (n \ "CONTACT_DATA_WITHOUT_RESPONSIBLE_NAME").headOption
        .orElse((n \ "CONTACT_DATA").headOption)
        .fold(NodeSeq.Empty)(node => NodeSeq.fromSeq(Seq(node)))
    ContactData(
      officialName =
        opt(cd, "ORGANISATION", "OFFICIALNAME").map(OfficialName(_)),
      address = opt(cd, "ADDRESS").map(StreetAddress(_)),
      town = opt(cd, "TOWN").map(TownName(_)),
      postalCode = opt(cd, "POSTAL_CODE").map(PostalCode(_)),
      country = {
        val v = atr(cd \ "COUNTRY", "VALUE")
        if v.nonEmpty then Some(CountryCode.toDomain(v)) else None
      },
      contactPoint = opt(cd, "CONTACT_POINT").map(RichText(_)),
      attention = opt(cd, "ATTENTION").map(RichText(_)),
      phone = opt(cd, "PHONE").map(PhoneNumber(_)),
      emails =
        (cd \ "E_MAILS" \ "E_MAIL").map(e => EmailAddress(e.text.trim)).toList,
      fax = opt(cd, "FAX").map(FaxNumber(_)),
      url = opt(cd, "URL").map(Url(_))
    )

  /** Parse TYPE_AND_ACTIVITIES element. */
  private def parseTypeAndActivities(n: NodeSeq): Option[TypeAndActivities] =
    val ta = (n \ "TYPE_AND_ACTIVITIES")
    if ta.isEmpty then None
    else
      Some(
        TypeAndActivities(
          authorityType =
            opt(ta, "TYPE_OF_CONTRACTING_AUTHORITY").map(RichText(_)),
          authorityOther =
            opt(ta, "TYPE_OF_CONTRACTING_AUTHORITY_OTHER").map(RichText(_)),
          activities = (ta \ "TYPE_OF_ACTIVITY")
            .map(e => RichText(atr(NodeSeq.fromSeq(Seq(e)), "VALUE")))
            .toList,
          activityOther = opt(ta, "TYPE_OF_ACTIVITY_OTHER").map(RichText(_))
        )
      )

  /** Parse PURCHASING_ON_BEHALF section. */
  private def parsePurchasingOnBehalf(n: NodeSeq): Option[PurchasingOnBehalf] =
    val pob = (n \\ "PURCHASING_ON_BEHALF").headOption
    pob match
      case None => None
      case Some(node) =>
        if (node \ "PURCHASING_ON_BEHALF_YES").nonEmpty then
          // R2.0.8: on-behalf contact lives in CONTACT_DATA_OTHER_BEHALF_CONTRACTING_AUTORITHY
          val obContacts = (node \\ "CONTACT_DATA_OTHER_BEHALF_CONTRACTING_AUTORITHY").map { p =>
            val ps = NodeSeq.fromSeq(Seq(p))
            ContactData(
              officialName = opt(ps, "ORGANISATION", "OFFICIALNAME").map(OfficialName(_)),
              address      = opt(ps, "ADDRESS").map(StreetAddress(_)),
              town         = opt(ps, "TOWN").map(TownName(_)),
              postalCode   = opt(ps, "POSTAL_CODE").map(PostalCode(_)),
              country      = {
                val v = atr(ps \ "COUNTRY", "VALUE")
                if v.nonEmpty then Some(CountryCode.toDomain(v)) else None
              },
              contactPoint = None, attention = None,
              phone = None, emails = Nil, fax = None, url = None
            )
          }.toList
          // Fallback: older schema variants use CA_CE_CONCESSIONAIRE_PROFILE
          val contacts =
            if obContacts.nonEmpty then obContacts
            else (node \\ "CA_CE_CONCESSIONAIRE_PROFILE")
              .map(p => parseContactData(NodeSeq.fromSeq(Seq(p))))
              .toList
          Some(PurchasingYes(contacts))
        else Some(PurchasingNo)

  /** Parse internet addresses block (URL_GENERAL, URL_BUYER, URL_INFORMATION,
    * URL_PARTICIPATE).
    */
  private def parseInternetAddresses(n: NodeSeq): Option[InternetAddresses] =
    val ia: NodeSeq = {
      val a: NodeSeq = n \\ "INTERNET_ADDRESSES_CONTRACT_AWARD"
      val b: NodeSeq = n \\ "INTERNET_ADDRESSES_CONTRACT"
      a.headOption
        .orElse(b.headOption)
        .fold(NodeSeq.Empty)(node => NodeSeq.fromSeq(Seq(node)))
    }
    if ia.isEmpty then None
    else
      Some(
        InternetAddresses(
          generalAddress = opt(ia, "URL_GENERAL").map(v => IaUrl.unsafe(v)),
          buyerProfileAddress = opt(ia, "URL_BUYER").map(v => IaUrl.unsafe(v)),
          eTendering = opt(ia, "URL_PARTICIPATE").map(v => IaUrl.unsafe(v))
        )
      )

  /** Parse LOCATION_NUTS — text + NUTS codes (handles both NUTS and
    * n2016:NUTS).
    */
  private def parseLocationNuts(n: NodeSeq): Option[LocationNuts] =
    val ln = (n \\ "LOCATION_NUTS").headOption
      .map(NodeSeq.fromSeq)
      .getOrElse(NodeSeq.Empty)
    if ln.isEmpty then None
    else
      Some(
        LocationNuts(
          locationText = opt(ln \ "LOCATION", "P").map(RichText(_)),
          nutsCodes =
            (ln \\ "NUTS").map(e => NutsCode.unsafe(e \@ "CODE")).toList
        )
      )

  /** Parse a contract value from COSTS_RANGE_AND_CURRENCY_WITH_VAT_RATE. The
    * schema uses INC_16 group → VALUE_COST | RANGE_VALUE_COST.
    */
  private def parseContractValue(n: NodeSeq): Option[FormContractValue] =
    val crwv = (n \\ "COSTS_RANGE_AND_CURRENCY_WITH_VAT_RATE").headOption
      .map(NodeSeq.fromSeq)
      .getOrElse(NodeSeq.Empty)
    if crwv.isEmpty then
      // INC_40 pattern: CURRENCY on wrapper element, FMTVAL on VALUE_COST child
      val initEl = (n \\ "INITIAL_ESTIMATED_TOTAL_VALUE_CONTRACT").headOption
      initEl match
        case Some(parent) =>
          val cur = Currency.from(parent \@ "CURRENCY")
          (NodeSeq.fromSeq(Seq(parent)) \\ "VALUE_COST").headOption.flatMap { el =>
            MonetaryAmount.fromString(el \@ "FMTVAL").toOption.map { amt =>
              ExactValue(ValueCost(amt, cur))
            }
          }
        case None =>
          // Direct VALUE_COST with its own CURRENCY attribute
          (n \\ "VALUE_COST").headOption.flatMap { el =>
            MonetaryAmount.fromString(el \@ "FMTVAL").toOption.map { amt =>
              ExactValue(ValueCost(amt, Currency.from(el \@ "CURRENCY")))
            }
          }
    else
      val cur = Currency.from(atr(crwv, "CURRENCY"))
      // Prefer VALUE_COST; fall back to RANGE_VALUE_COST
      val vcEl = (crwv \\ "VALUE_COST").headOption
      val rvEl = (crwv \\ "RANGE_VALUE_COST").headOption
      vcEl
        .flatMap { el =>
          MonetaryAmount.fromString(el \@ "FMTVAL").toOption.map { amt =>
            ExactValue(ValueCost(amt, cur))
          }
        }
        .orElse {
          for
            rv <- rvEl
            low <- MonetaryAmount
              .fromString(atr(rv \ "LOW_VALUE", "FMTVAL"))
              .toOption
            high <- MonetaryAmount
              .fromString(atr(rv \ "HIGH_VALUE", "FMTVAL"))
              .toOption
          yield RangeValue(RangeValueCost(low, high, cur))
        }

  /** Parse TOTAL_FINAL_VALUE (Section II aggregate value). */
  private def parseTotalFinalValue(n: NodeSeq): Option[TotalFinalValue] =
    parseContractValue(n \\ "TOTAL_FINAL_VALUE").map(TotalFinalValue(_))

  /** Parse INITIAL_ESTIMATED_TOTAL_VALUE_CONTRACT (INC_40 group). */
  private def parseEstimatedValue(n: NodeSeq): Option[FormContractValue] =
    val el = (n \\ "INITIAL_ESTIMATED_TOTAL_VALUE_CONTRACT").headOption
    el.flatMap { e =>
      val fmtval = e \@ "FMTVAL"
      val cur = Currency.from(e \@ "CURRENCY")
      MonetaryAmount
        .fromString(fmtval)
        .toOption
        .map(amt => ExactValue(ValueCost(amt, cur)))
    }

  /** Parse AWARD_OF_CONTRACT / AWARD_OF_CONTRACT_DEFENCE — one lot entry. */
  private def parseAwardNode(a: Node): AwardOfContract =
    val n = NodeSeq.fromSeq(Seq(a))
    val eona = (n \ "ECONOMIC_OPERATOR_NAME_ADDRESS")
    AwardOfContract(
      item = opt(n, "ITEM").map(RichText(_)),
      contractNumber = opt(n, "CONTRACT_NUMBER").map(RichText(_)),
      lotNumbers = (n \ "LOT_NUMBER").map(e => RichText(e.text.trim)).toList,
      contractTitle = opt(deep(n, "CONTRACT_TITLE"), "P").map(RichText(_)),
      awardDate = structDateAt(n, "CONTRACT_AWARD_DATE"),
      offersReceived = opt(n, "OFFERS_RECEIVED_NUMBER").flatMap(_.toIntOption),
      offersReceivedMeaning =
        opt(n, "OFFERS_RECEIVED_NUMBER_MEANING").flatMap(_.toIntOption),
      winner = if eona.nonEmpty then Some(parseContactData(eona)) else None,
      contractValue = parseContractValue(n \\ "CONTRACT_VALUE_INFORMATION")
        .map(v => ContractValueInformation(None, Some(v), None, None)),
      subcontracted = {
        val sub = (n \\ "MORE_INFORMATION_TO_SUB_CONTRACTED").headOption
        sub.map(s => (s \ "CONTRACT_LIKELY_SUB_CONTRACTED").nonEmpty)
      }
    )

  /** Parse PROCEDURES_FOR_APPEAL (Section VI). */
  private def parseProceduresForAppeal(
      n: NodeSeq
  ): Option[ProceduresForAppeal] =
    val pfa = (n \\ "PROCEDURES_FOR_APPEAL").headOption
      .map(NodeSeq.fromSeq)
      .getOrElse(NodeSeq.Empty)
    if pfa.isEmpty then None
    else
      Some(
        ProceduresForAppeal(
          appealBody =
            Some(parseContactData(pfa \ "APPEAL_PROCEDURE_BODY_RESPONSIBLE")),
          mediationBody = {
            val m = pfa \ "MEDIATION_PROCEDURE_BODY_RESPONSIBLE"
            if m.nonEmpty then Some(parseContactData(m)) else None
          },
          lodgingAppeals =
            opt(pfa, "LODGING_OF_APPEALS", "LODGING_OF_APPEALS_PRECISION").map(
              RichText(_)
            ),
          lodgingInfoForService = {
            val l = pfa \ "LODGING_INFORMATION_FOR_SERVICE"
            if l.nonEmpty then Some(parseContactData(l)) else None
          }
        )
      )

  /** Parse COMPLEMENTARY_INFORMATION_CONTRACT_AWARD (Section VI of award
    * forms).
    */
  private def parseComplementaryAward(
      section: NodeSeq
  ): ComplementaryInfoAward =
    val n: NodeSeq = {
      val a: NodeSeq = section \\ "COMPLEMENTARY_INFORMATION_CONTRACT_AWARD"
      val b: NodeSeq = section \\ "COMPLEMENTARY_INFORMATION_CONTRACT_AWARD_UTILITIES"
      a.headOption
        .orElse(b.headOption)
        .fold(section)(node => NodeSeq.fromSeq(Seq(node)))
    }
    ComplementaryInfoAward(
      relatesToEuProject = {
        if (n \\ "RELATES_TO_EU_PROJECT_YES").nonEmpty then Some(true)
        else if (n \\ "RELATES_TO_EU_PROJECT_NO").nonEmpty then Some(false)
        else None
      },
      additionalInfo = opt(n \\ "ADDITIONAL_INFORMATION", "P").map(RichText(_)),
      proceduresForAppeal = parseProceduresForAppeal(n),
      dispatchDate = structDateAt(n, "NOTICE_DISPATCH_DATE")
    )

  /** Parse COMPLEMENTARY_INFORMATION_CONTRACT_NOTICE (Section VI of contract
    * notice forms).
    */
  private def parseComplementaryNotice(
      section: NodeSeq
  ): ComplementaryInfoNotice =
    val n = (section \\ "COMPLEMENTARY_INFORMATION_CONTRACT_NOTICE").headOption
      .map(NodeSeq.fromSeq)
      .getOrElse(section)
    ComplementaryInfoNotice(
      relatesToEuProject = {
        if (n \\ "RELATES_TO_EU_PROJECT_YES").nonEmpty then Some(true)
        else if (n \\ "RELATES_TO_EU_PROJECT_NO").nonEmpty then Some(false)
        else None
      },
      additionalInfo = opt(n \\ "ADDITIONAL_INFORMATION", "P").map(RichText(_)),
      proceduresForAppeal = parseProceduresForAppeal(n),
      dispatchDate = structDateAt(n, "NOTICE_DISPATCH_DATE")
    )

  /** Parse previous publication info — common across F3/F6/F15/F18 variants.
    * All variants share: CNT_NOTICE_INFORMATION → NOTICE_NUMBER_OJ + DATE_OJ.
    */
  private def parsePreviousPublication(
      n: NodeSeq
  ): Option[PreviousPublication] =
    val cnt = (n \\ "CNT_NOTICE_INFORMATION").headOption.map(NodeSeq.fromSeq)
    cnt.flatMap { c =>
      opt(c, "NOTICE_NUMBER_OJ").flatMap { num =>
        NoticeNumber.from(num).toOption.map { nn =>
          PreviousPublication(
            noticeNumber = nn,
            date = structDateAt(c, "DATE_OJ"),
            noticeType = {
              val choice = atr(c \\ "CNT_NOTICE_INFORMATION_S", "CHOICE")
              choice match
                case "CONTRACT_NOTICE"          => Some(ContractNoticeRef)
                case "PRIOR_INFORMATION_NOTICE" => Some(PriorInfoNoticeRef)
                case "NOTICE_BUYER_PROFILE" => Some(PrevNoticeBuyerProfileRef)
                case _                      => None
            }
          )
        }
      }
    }

  /** Detect procedure type from any TYPE_OF_PROCEDURE_DEF or TYPE_OF_PROCEDURE
    * block.
    */
  private val ProcedureTags = Seq(
    "PT_OPEN" -> OpenProcedure,
    "PT_RESTRICTED" -> RestrictedProcedure,
    "PT_ACCELERATED_RESTRICTED" -> AcceleratedRestricted,
    "PT_COMPETITIVE_DIALOGUE" -> CompetitiveDialogue,
    "PT_NEGOTIATED_WITH_COMPETITION" -> NegotiatedWithCompetition,
    "PT_ACCELERATED_NEGOTIATED" -> AcceleratedNegotiated,
    "F03_PT_NEGOTIATED_WITHOUT_COMPETITION" -> NegotiatedWithoutCompetition(None),
    "F03_AWARD_WITHOUT_PRIOR_PUBLICATION"   -> AwardWithoutPriorPublication(None),
    "F06_PT_NEGOTIATED_WITHOUT_COMPETITION" -> NegotiatedWithoutCompetition(None),
    "F15_PT_NEGOTIATED_WITHOUT_COMPETITION" -> NegotiatedWithoutCompetition(None),
    "F18_PT_NEGOTIATED_WITHOUT_COMPETITION" -> NegotiatedWithoutCompetition(None)
  )

  private def detectProcedureType(n: NodeSeq): Option[ProcedureType] =
    ProcedureTags.collectFirst {
      case (tag, pt) if (n \\ tag).nonEmpty => pt
    }

  /** Parse @LG, @CATEGORY, @VERSION, @CTYPE from a form root element. */
  private def parseFormMeta(formEl: Node): FormMeta =
    FormMeta(
      language = Language.from(formEl \@ "LG"),
      category = FormCategory
        .from(formEl \@ "CATEGORY")
        .getOrElse(FormCategory.Original),
      version = Option(formEl \@ "VERSION").filter(_.nonEmpty).map(RichText(_)),
      contractType = ContractType.from(
        (formEl.child
          .collectFirst { case e: Elem => e })
          .map(_ \@ "CTYPE")
          .getOrElse("")
      )
    )

  // ─────────────────────────────────────────────────────────────────────────────
  //  1. TECHNICAL SECTION
  // ─────────────────────────────────────────────────────────────────────────────

  private def parseTechnical(root: Elem): Either[ParseError, TechnicalSection] =
    val tech = root \ "TECHNICAL_SECTION"
    for
      recId <- req(
        txt(tech, "RECEPTION_ID"),
        "RECEPTION_ID",
        "TECHNICAL_SECTION"
      )
      delDate <- req(
        txt(tech, "DELETION_DATE"),
        "DELETION_DATE",
        "TECHNICAL_SECTION"
      )
      lgList <- req(
        txt(tech, "FORM_LG_LIST"),
        "FORM_LG_LIST",
        "TECHNICAL_SECTION"
      )
    yield TechnicalSection(
      receptionId = ReceptionId.unsafe(recId),
      deletionDate = TedDate.unsafe(delDate),
      formLanguages = lgList.split("\\s+").map(Language.from).toList,
      comments = opt(tech, "COMMENTS").map(RichText(_)),
      oldHeading = opt(tech, "OLD_HEADING").map(HeadingCode(_))
    )

  // ─────────────────────────────────────────────────────────────────────────────
  //  2. LINKS SECTION
  // ─────────────────────────────────────────────────────────────────────────────

  private def parseLink(n: NodeSeq, name: String): Option[Link] =
    val el = n \ name
    if el.isEmpty then None
    else
      Some(
        Link(
          href = Url(atr(el, "href")),
          linkType = atr(el, "type"),
          title = opt(el, "title").map(RichText(_))
        )
      )

  private def parseLinks(root: Elem): LinksSection =
    val ls = root \ "LINKS_SECTION"
    LinksSection(
      xmlSchemaDefinitionLink = parseLink(ls, "XML_SCHEMA_DEFINITION_LINK"),
      officialFormsLink = parseLink(ls, "OFFICIAL_FORMS_LINK"),
      formsLabelsLink = parseLink(ls, "FORMS_LABELS_LINK"),
      originalCpvLink = parseLink(ls, "ORIGINAL_CPV_LINK"),
      originalNutsLink = parseLink(ls, "ORIGINAL_NUTS_LINK")
    )

  // ─────────────────────────────────────────────────────────────────────────────
  //  3. SENDER  (optional)
  // ─────────────────────────────────────────────────────────────────────────────

  private def parseSender(root: Elem): Option[Sender] =
    val s = root \ "SENDER"
    if s.isEmpty then None
    else
      val login = Login(
        esenderLogin = RichText(txt(s, "LOGIN", "ESENDER_LOGIN")),
        customerLogin = opt(s, "LOGIN", "CUSTOMER_LOGIN").map(RichText(_)),
        loginClass = Option(atr(s \ "LOGIN", "CLASS"))
          .filter(_.nonEmpty)
          .map {
            case "A" => LoginClass.A; case "B" => LoginClass.B
            case "C" => LoginClass.C; case "D" => LoginClass.D
            case _   => LoginClass.Unknown
          }
      )
      val user = {
        val u = s \ "USER"
        if u.isEmpty then None
        else
          Some(
            SenderUser(
              organisation = opt(u, "ORGANISATION").map(OfficialName(_)),
              attention = opt(u, "ATTENTION").map(RichText(_)),
              address = opt(u, "ADDRESS").map(StreetAddress(_)),
              postalCode = opt(u, "POSTAL_CODE").map(PostalCode(_)),
              town = opt(u, "TOWN").map(TownName(_)),
              country = {
                val v = atr(u \ "COUNTRY", "VALUE")
                if v.nonEmpty then Some(CountryCode.toDomain(v)) else None
              },
              phone = opt(u, "PHONE").map(PhoneNumber(_)),
              fax = opt(u, "FAX").map(FaxNumber(_)),
              email = opt(u, "USER_E_MAILS", "E_MAIL").map(EmailAddress(_)),
              url = opt(u, "URL").map(Url(_))
            )
          )
      }
      Some(Sender(login, user, ExternalDocRef(txt(s, "NO_DOC_EXT"))))

  // ─────────────────────────────────────────────────────────────────────────────
  //  4. CODED DATA SECTION
  // ─────────────────────────────────────────────────────────────────────────────

  private def parseCodedDataSection(
      root: Elem
  ): Either[ParseError, CodedDataSection] =
    val cds = root \ "CODED_DATA_SECTION"
    for
      refOjs <- parseRefOjs(cds)
      noticeData <- parseNoticeData(cds)
      codifData <- parseCodifData(cds)
    yield CodedDataSection(refOjs, noticeData, codifData)

  private def parseRefOjs(cds: NodeSeq): Either[ParseError, RefOjs] =
    val n = cds \ "REF_OJS"
    val ojNo = atr(n \ "NO_OJ", "CLASS") // grab class from sibling
    val noOj = txt(n, "NO_OJ")
    val datePub = txt(n, "DATE_PUB")
    for
      num <- req(noOj, "NO_OJ", "REF_OJS")
      date <- req(datePub, "DATE_PUB", "REF_OJS")
    yield RefOjs(
      number = OjNumber(
        OjIssueNumber.unsafe(num),
        OjClass.from(atr(n \ "NO_OJ", "CLASS")),
        atr(n \ "NO_OJ", "LAST") == "YES"
      ),
      publicationDate = TedDate.unsafe(date)
    )

  private def parseNoticeData(cds: NodeSeq): Either[ParseError, NoticeData] =
    val nd = cds \ "NOTICE_DATA"
    Right(
      NoticeData(
        noDocOjs = opt(nd, "NO_DOC_OJS").flatMap(NoticeNumber.from(_).toOption),
        uriList = {
          val uris = (nd \ "URI_LIST" \ "URI_DOC")
            .map(e =>
              UriDoc(Language.from(e \@ "LG"), IaUrl.unsafe(e.text.trim))
            )
            .toList
          if uris.isEmpty then None else Some(uris)
        },
        originalLanguage = Language.from(txt(nd, "LG_ORIG")),
        isoCountry = CountryCode.toDomain(atr(nd \ "ISO_COUNTRY", "VALUE")),
        iaUrlGeneral = IaUrl.unsafe(txt(nd, "IA_URL_GENERAL")),
        iaUrlEtendering = opt(nd, "IA_URL_ETENDERING").map(IaUrl.unsafe),
        originalCpvCodes = (nd \ "ORIGINAL_CPV").map { e =>
          CpvEntry(CpvCode.unsafe(e \@ "CODE"), RichText(e.text.trim))
        }.toList,
        currentCpvCodes = (nd \ "CURRENT_CPV").map { e =>
          CpvEntry(CpvCode.unsafe(e \@ "CODE"), RichText(e.text.trim))
        }.toList,
        originalNutsCodes = (nd \ "ORIGINAL_NUTS").map { e =>
          NutsEntry(NutsCode.unsafe(e \@ "CODE"), RichText(e.text.trim))
        }.toList,
        currentNutsCodes = (nd \ "CURRENT_NUTS").map { e =>
          NutsEntry(NutsCode.unsafe(e \@ "CODE"), RichText(e.text.trim))
        }.toList,
        valuesList = {
          val vs = (nd \ "VALUES_LIST" \ "VALUES").map { e =>
            val vtype =
              ValuesType.from(e \@ "TYPE").getOrElse(ValuesType.Global)
            if (e \ "SINGLE_VALUE").nonEmpty then
              val v = e \ "SINGLE_VALUE" \ "VALUE"
              SingleValueEntry(
                vtype,
                NoticeValue(
                  amount = MonetaryAmount
                    .fromString((v).text.trim)
                    .getOrElse(MonetaryAmount.unsafe(0)),
                  currency = Option(Currency.from((v \@ "CURRENCY")))
                    .filter(_ != Currency.Unknown(""))
                    .orElse(None),
                  format = Option(v \@ "FORMAT").filter(_.nonEmpty)
                )
              )
            else
              val vals = (e \ "RANGE_VALUE" \ "VALUE").map { v =>
                NoticeValue(
                  amount = MonetaryAmount
                    .fromString(v.text.trim)
                    .getOrElse(MonetaryAmount.unsafe(0)),
                  currency = Option(Currency.from(v \@ "CURRENCY"))
                    .filter(_ != Currency.Unknown(""))
                    .orElse(None),
                  format = Option(v \@ "FORMAT").filter(_.nonEmpty)
                )
              }.toList
              RangeValueEntry(vtype, vals)
          }.toList
          if vs.isEmpty then None else Some(vs)
        },
        refNotice = {
          val rn = nd \ "REF_NOTICE"
          if rn.isEmpty then None
          else
            Some(RefNotice((rn \ "NO_DOC_OJS").flatMap { e =>
              NoticeNumber.from(e.text.trim).toOption
            }.toList))
        }
      )
    )

  private def parseCodifData(cds: NodeSeq): Either[ParseError, CodifData] =
    val cd = cds \ "CODIF_DATA"
    for dispDate <- req(
        txt(cd, "DS_DATE_DISPATCH"),
        "DS_DATE_DISPATCH",
        "CODIF_DATA"
      )
    yield CodifData(
      dispatchDate = TedDate.unsafe(dispDate),
      documentRequestDate =
        opt(cd, "DD_DATE_REQUEST_DOCUMENT").map(TedDateTime.unsafe),
      submissionDate =
        opt(cd, "DT_DATE_FOR_SUBMISSION").map(TedDateTime.unsafe),
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
      heading = HeadingCode(txt(cd, "HEADING")),
      directive = opt(cd, "DIRECTIVE")
        .flatMap(v => Directive.from(atr(cd \ "DIRECTIVE", "VALUE")))
    )

  // ─────────────────────────────────────────────────────────────────────────────
  //  5. TRANSLATION SECTION
  // ─────────────────────────────────────────────────────────────────────────────

  private def parseTranslationSection(root: Elem): TranslationSection =
    val ts = root \ "TRANSLATION_SECTION"
    TranslationSection(
      titles = (ts \ "ML_TITLES" \ "ML_TI_DOC").map { e =>
        TitleTranslation(
          language = Language.from(e \@ "LG"),
          country = opt(NodeSeq.fromSeq(Seq(e)), "TI_CY").map(RichText(_)),
          town = opt(NodeSeq.fromSeq(Seq(e)), "TI_TOWN").map(TownName(_)),
          text = opt(NodeSeq.fromSeq(Seq(e)), "TI_TEXT").map(RichText(_))
        )
      }.toList,
      authorityNames = (ts \ "ML_AA_NAMES" \ "AA_NAME").map { e =>
        AuthorityNameTranslation(
          language = Language.from(e \@ "LG"),
          name = OfficialName(e.text.trim)
        )
      }.toList,
      transliterations = {
        val tr = (ts \ "TRANSLITERATIONS" \ "TRANSLITERATED_ADDR")
        if tr.isEmpty then None
        else
          Some(
            TransliteratedAddress(
              organisation =
                opt(tr, "ORGANISATION", "OFFICIALNAME").map(OfficialName(_)),
              address = StreetAddress(txt(tr, "ADDRESS")),
              town = TownName(txt(tr, "TOWN")),
              postalCode = opt(tr, "POSTAL_CODE").map(PostalCode(_)),
              country = CountryCode.toDomain(atr(tr \ "COUNTRY", "VALUE")),
              contactPoint = opt(tr, "CONTACT_POINT").map(RichText(_)),
              attention = opt(tr, "ATTENTION").map(RichText(_)),
              phone = opt(tr, "PHONE").map(PhoneNumber(_)),
              email = (tr \ "E_MAILS" \ "E_MAIL").headOption
                .map(e => EmailAddress(e.text.trim))
                .orElse(opt(tr, "E_MAIL").map(EmailAddress(_))),
              fax = opt(tr, "FAX").map(FaxNumber(_))
            )
          )
      }
    )

  // ─────────────────────────────────────────────────────────────────────────────
  //  6. FORM SECTION
  // ─────────────────────────────────────────────────────────────────────────────

  private def parseFormSection(root: Elem): Either[ParseError, FormSection] =
    val formElems = (root \ "FORM_SECTION").headOption.toList
      .flatMap(_.child.collect { case e: Elem => e })
    val results = formElems.map(parseFormBody)
    // collect errors — fail the notice if any language version fails
    val errors = results.collect { case Left(e) => e }
    if errors.nonEmpty then Left(errors.head)
    else Right(FormSection(results.collect { case Right(fb) => fb }))

  private def parseFormBody(formEl: Node): Either[ParseError, FormBody] =
    val meta = parseFormMeta(formEl)
    // fd_ is the first Elem child of the form root element
    val fd = NodeSeq.fromSeq(formEl.child.collect { case e: Elem => e })
    formEl.label match
      case "PRIOR_INFORMATION" =>
        parseF01(fd, meta).map(PriorInformationForm(_))
      case "CONTRACT"       => parseF02(fd, meta).map(ContractNoticeForm(_))
      case "CONTRACT_AWARD" => parseF03(fd, meta).map(ContractAwardForm(_))
      case "PERIODIC_INDICATIVE_UTILITIES" =>
        parseF04(fd, meta).map(PeriodicIndicativeUtilitiesForm(_))
      case "CONTRACT_UTILITIES" =>
        parseF05(fd, meta).map(ContractUtilitiesForm(_))
      case "CONTRACT_AWARD_UTILITIES" =>
        parseF06(fd, meta).map(ContractAwardUtilitiesForm(_))
      case "QUALIFICATION_SYSTEM_UTILITIES" =>
        parseF07(fd, meta).map(QualificationSystemUtilitiesForm(_))
      case "BUYER_PROFILE" => parseF08(fd, meta).map(BuyerProfileForm(_))
      case "SIMPLIFIED_CONTRACT" =>
        parseF09(fd, meta).map(SimplifiedContractForm(_))
      case "CONCESSION" => parseF10(fd, meta).map(ConcessionForm(_))
      case "CONTRACT_CONCESSIONAIRE" =>
        parseF11(fd, meta).map(ContractConcessionaireForm(_))
      case "DESIGN_CONTEST" => parseF12(fd, meta).map(DesignContestForm(_))
      case "RESULT_DESIGN_CONTEST" =>
        parseF13(fd, meta).map(ResultDesignContestForm(_))
      case "ADDITIONAL_INFORMATION_CORRIGENDUM" =>
        parseF14(fd, meta).map(AdditionalInformationCorrigendumForm(_))
      case "VOLUNTARY_EX_ANTE_TRANSPARENCY_NOTICE" =>
        parseF15(fd, meta).map(VeatForm(_))
      case "PRIOR_INFORMATION_DEFENCE" =>
        parseF16(fd, meta).map(PriorInformationDefenceForm(_))
      case "CONTRACT_DEFENCE" => parseF17(fd, meta).map(ContractDefenceForm(_))
      case "CONTRACT_AWARD_DEFENCE" =>
        parseF18(fd, meta).map(ContractAwardDefenceForm(_))
      case "CONTRACT_CONCESSIONAIRE_DEFENCE" =>
        parseF19(fd, meta).map(ContractSubDefenceForm(_))
      case "PRIOR_INFORMATION_MOVE" =>
        parseT01(fd, meta).map(PriorInformationMoveForm(_))
      case "CONTRACT_MOVE" => parseT02(fd, meta).map(ContractMoveForm(_))
      case "OTH_NOT" =>
        Right(
          OtherNoticeForm(
            OthNot(meta.language, meta.category, RichText(fd.text.trim))
          )
        )
      case "EEIG" =>
        Right(
          EeigForm(
            Eeig(
              meta.language,
              meta.category,
              Nil,
              Some(RichText(fd.text.trim))
            )
          )
        )
      case other => Left(ParseError.UnknownFormType(other))

  // ─────────────────────────────────────────────────────────────────────────────
  //  7. Form parsers
  // ─────────────────────────────────────────────────────────────────────────────

  // ── F01 ─────────────────────────────────────────────────────────────────────

  private def parseF01(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F01PriorInformation] =
    val auth = fd \ "AUTHORITY_PRIOR_INFORMATION"
    val obj = fd \ "OBJECT_WORKS_PRIOR_INFORMATION" match
      case n if n.nonEmpty => n
      case _               => fd \ "OBJECT_SUPPLIES_SERVICES_PRIOR_INFORMATION"
    val lefti = fd \ "LEFTI_PRIOR_INFORMATION"
    val comp = fd \ "OTH_INFO_PRIOR_INFORMATION"
    Right(
      F01PriorInformation(
        meta = meta,
        contractingAuthority = parseContractingProfile(auth),
        typeAndActivities = parseTypeAndActivities(
          auth \\ "TYPE_AND_ACTIVITIES_AND_PURCHASING_ON_BEHALF"
        ),
        purchasingOnBehalf = parsePurchasingOnBehalf(auth),
        contractObject = F01ContractObject(
          contractType =
            ContractType.from(atr(obj \\ "TYPE_CONTRACT", "VALUE")),
          description = opt(obj \\ "SHORT_DESCRIPTION", "P").map(RichText(_)),
          cpv = {
            val code = atr(obj \\ "CPV_MAIN" \ "CPV_CODE", "CODE")
            if code.nonEmpty then
              Some(
                CpvCodes(
                  CpvCode.unsafe(code),
                  (obj \\ "CPV_ADDITIONAL" \ "CPV_CODE")
                    .map(e => CpvCode.unsafe(e \@ "CODE"))
                    .toList
                )
              )
            else None
          },
          lots = (obj \ "LOT_PRIOR_INFORMATION").map { lot =>
            LotPriorInformation(
              lotNumber =
                opt(NodeSeq.fromSeq(Seq(lot)), "LOT_NUMBER").map(RichText(_)),
              lotTitle =
                opt(NodeSeq.fromSeq(Seq(lot)), "LOT_TITLE").map(RichText(_)),
              lotDescription =
                opt(NodeSeq.fromSeq(Seq(lot)), "LOT_DESCRIPTION", "P")
                  .map(RichText(_)),
              cpv = None,
              natureQtyScope =
                opt(NodeSeq.fromSeq(Seq(lot)), "NATURE_QUANTITY_SCOPE", "P")
                  .map(RichText(_)),
              scheduledDate = None,
              additionalInfo =
                opt(NodeSeq.fromSeq(Seq(lot)), "ADDITIONAL_INFORMATION", "P")
                  .map(RichText(_))
            )
          }.toList,
          estimatedValue = parseEstimatedValue(obj)
        ),
        lefti =
          if lefti.isEmpty then None
          else
            Some(
              F01Lefti(
                personalSituation =
                  opt(lefti, "PERSONAL_SITUATION_OF_OPERATORS", "P").map(
                    RichText(_)
                  ),
                economicCapacity =
                  opt(lefti, "ECONOMIC_FINANCIAL_CAPACITY", "P").map(
                    RichText(_)
                  ),
                technicalCapacity =
                  opt(lefti, "TECHNICAL_CAPACITY_LEFTI", "P").map(RichText(_))
              )
            )
        ,
        complementaryInfo = F01ComplementaryInfo(
          relatesToEuProject =
            if (comp \\ "RELATES_TO_EU_PROJECT_YES").nonEmpty then Some(true)
            else if (comp \\ "RELATES_TO_EU_PROJECT_NO").nonEmpty then
              Some(false)
            else None,
          additionalInfo =
            opt(comp \\ "ADDITIONAL_INFORMATION", "P").map(RichText(_)),
          informationRegulatoryFramework =
            opt(comp, "INFORMATION_REGULATORY_FRAMEWORK").map(RichText(_)),
          dispatchDate = structDateAt(comp, "NOTICE_DISPATCH_DATE")
        )
      )
    )

  // ── F02 ─────────────────────────────────────────────────────────────────────

  private def parseF02(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F02ContractNotice] =
    val auth = fd \ "CONTRACTING_AUTHORITY_INFORMATION"
    val obj =
      fd \ "OBJECT_CONTRACT_INFORMATION" \ "DESCRIPTION_CONTRACT_INFORMATION"
    val lefti = fd \ "LEFTI_CONTRACT"
    val proc = fd \ "PROCEDURE_DEFINITION_CONTRACT_NOTICE"
    val comp = fd \ "COMPLEMENTARY_INFORMATION_CONTRACT_NOTICE"
    Right(
      F02ContractNotice(
        meta = meta,
        contractingAuthority = parseContractingProfile(auth),
        internetAddresses = {
          val ia = auth \ "INTERNET_ADDRESSES_CONTRACT"
          if ia.isEmpty then None
          else
            Some(
              InternetAddresses(
                generalAddress = opt(ia, "URL_GENERAL").map(IaUrl.unsafe),
                buyerProfileAddress = opt(ia, "URL_BUYER").map(IaUrl.unsafe),
                eTendering = opt(ia, "URL_PARTICIPATE").map(IaUrl.unsafe)
              )
            )
        },
        furtherInfo = {
          val fi = auth \\ "FURTHER_INFORMATION" \ "CONTACT_DATA"
          if fi.isEmpty then None else Some(parseContactData(fi))
        },
        specsAndDocuments = {
          val sd =
            auth \\ "SPECIFICATIONS_AND_ADDITIONAL_DOCUMENTS" \ "CONTACT_DATA"
          if sd.isEmpty then None else Some(parseContactData(sd))
        },
        tendersTo = {
          val tt =
            auth \\ "TENDERS_REQUESTS_APPLICATIONS_MUST_BE_SENT_TO" \ "CONTACT_DATA"
          if tt.isEmpty then None else Some(parseContactData(tt))
        },
        typeAndActivities = parseTypeAndActivities(
          auth \\ "TYPE_AND_ACTIVITIES_AND_PURCHASING_ON_BEHALF"
        ),
        purchasingOnBehalf = parsePurchasingOnBehalf(auth),
        contractObject = F02ContractObject(
          title = opt(obj, "TITLE_CONTRACT", "P").map(RichText(_)),
          contractType =
            ContractType.from(atr(obj \\ "TYPE_CONTRACT", "VALUE")),
          suppliesType =
            SuppliesType.from(atr(obj \\ "TYPE_SUPPLIES_CONTRACT", "VALUE")),
          location = parseLocationNuts(obj),
          noticeInvolves = opt(obj, "NOTICE_INVOLVES_DESC", "NOTICE_INVOLVES")
            .map(RichText(_)),
          description = RichText(txt(obj, "SHORT_CONTRACT_DESCRIPTION", "P")),
          cpv = {
            val code = atr(obj \\ "CPV_MAIN" \ "CPV_CODE", "CODE")
            if code.nonEmpty then
              Some(
                CpvCodes(
                  CpvCode.unsafe(code),
                  (obj \\ "CPV_ADDITIONAL" \ "CPV_CODE")
                    .map(e => CpvCode.unsafe(e \@ "CODE"))
                    .toList
                )
              )
            else None
          },
          contractCoveredGpa = {
            val gpa = obj \\ "CONTRACT_COVERED_GPA"
            if gpa.isEmpty then None else Some(atr(gpa, "VALUE") != "NO")
          },
          totalQuantityScope =
            opt(obj \\ "QUANTITY_SCOPE", "NATURE_QUANTITY_SCOPE", "P")
              .map(RichText(_)),
          lots =
            Nil, // lots are within fd \ "OBJECT_CONTRACT_INFORMATION" \ "LOT_DIVISION"
          estimatedValue = parseEstimatedValue(obj),
          options = opt(obj \\ "OPTIONS", "P").map(RichText(_)),
          recurrentContract = {
            if (comp \\ "RECURRENT_PROCUREMENT").nonEmpty then Some(true)
            else if (comp \\ "NO_RECURRENT_PROCUREMENT").nonEmpty then
              Some(false)
            else None
          }
        ),
        lefti = F02Lefti(
          personalSituation =
            opt(lefti, "PERSONAL_SITUATION_OF_OPERATORS", "P").map(RichText(_)),
          economicCapacity =
            opt(lefti, "ECONOMIC_FINANCIAL_CAPACITY", "P").map(RichText(_)),
          technicalCapacity =
            opt(lefti, "TECHNICAL_CAPACITY_LEFTI", "P").map(RichText(_)),
          contractConditions =
            opt(lefti, "CONTRACT_RELATING_CONDITIONS", "P").map(RichText(_)),
          reservedContracts = {
            if (lefti \\ "RESTRICTED_SHELTERED_WORKSHOP").nonEmpty then
              Some(true)
            else if (lefti \\ "RESTRICTED_SHELTERED_PROGRAM").nonEmpty then
              Some(true)
            else None
          }
        ),
        procedure = F02Procedure(
          procedureType = detectProcedureType(proc),
          awardCriteria =
            None, // TODO: parse AWARD_CRITERIA_CONTRACT_NOTICE_INFORMATION
          electronicAuction = {
            val ea = proc \\ "IF_AWARD_CRITERIA_ELECTRONIC"
            if ea.nonEmpty then Some(true)
            else if (proc \\ "NO_ELECTRONIC_AUCTION_USABLE").nonEmpty then
              Some(false)
            else None
          },
          fileReference = opt(
            proc,
            "ADMINISTRATIVE_INFORMATION_CONTRACT_NOTICE",
            "FILE_REFERENCE_NUMBER",
            "P"
          ).map(FileReference(_)),
          previousPublication = parsePreviousPublication(proc),
          receiptDeadline = opt(
            proc,
            "ADMINISTRATIVE_INFORMATION_CONTRACT_NOTICE",
            "RECEIPT_LIMIT_DATE"
          ).map(TedDateTime.unsafe),
          participateDeadline = None,
          minimumTimeMainTender = None,
          openingConditions = None
        ),
        complementaryInfo = parseComplementaryNotice(comp)
      )
    )

  // ── F03 ─────────────────────────────────────────────────────────────────────

  private def parseF03(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F03ContractAward] =
    val auth = fd \ "CONTRACTING_AUTHORITY_INFORMATION_CONTRACT_AWARD"
    val obj = fd \ "OBJECT_CONTRACT_INFORMATION_CONTRACT_AWARD_NOTICE"
    val proc = fd \ "PROCEDURE_DEFINITION_CONTRACT_AWARD_NOTICE"
    val awards = fd \ "AWARD_OF_CONTRACT"
    val comp = fd \ "COMPLEMENTARY_INFORMATION_CONTRACT_AWARD"
    Right(
      F03ContractAward(
        meta = meta,
        contractingAuthority = parseContractingProfile(auth),
        internetAddresses = parseInternetAddresses(auth),
        typeAndActivities = parseTypeAndActivities(
          auth \\ "TYPE_AND_ACTIVITIES_AND_PURCHASING_ON_BEHALF"
        ),
        purchasingOnBehalf = parsePurchasingOnBehalf(auth),
        contractObject = {
          val desc = obj \ "DESCRIPTION_AWARD_NOTICE_INFORMATION"
          F03ContractObject(
            title = opt(desc \\ "TITLE_CONTRACT", "P").map(RichText(_)),
            contractType = ContractType.from(
              atr(
                desc \\ "TYPE_CONTRACT_LOCATION_W_PUB" \ "TYPE_CONTRACT",
                "VALUE"
              )
            ),
            suppliesType = SuppliesType.from(
              atr(
                desc \\ "TYPE_CONTRACT_LOCATION_W_PUB" \ "TYPE_SUPPLIES_CONTRACT",
                "VALUE"
              )
            ),
            location = parseLocationNuts(desc),
            noticeInvolves =
              opt(desc, "NOTICE_INVOLVES_DESC", "NOTICE_INVOLVES").map(
                RichText(_)
              ),
            description =
              RichText(txt(desc, "SHORT_CONTRACT_DESCRIPTION", "P")),
            cpv = {
              val code = atr(desc \\ "CPV_MAIN" \ "CPV_CODE", "CODE")
              if code.nonEmpty then
                Some(
                  CpvCodes(
                    CpvCode.unsafe(code),
                    (desc \\ "CPV_ADDITIONAL" \ "CPV_CODE")
                      .map(e => CpvCode.unsafe(e \@ "CODE"))
                      .toList
                  )
                )
              else None
            },
            contractCoveredGpa = {
              val gpa = desc \\ "CONTRACT_COVERED_GPA"
              if gpa.isEmpty then None else Some(atr(gpa, "VALUE") != "NO")
            },
            totalFinalValue = parseTotalFinalValue(obj)
          )
        },
        procedure = F03Procedure(
          procedureType = detectProcedureType(proc),
          electronicAuction = {
            val ea = proc \\ "F03_IS_ELECTRONIC_AUCTION_USABLE"
            if ea.isEmpty then None else Some(atr(ea, "VALUE") == "YES")
          },
          fileReference = opt(
            proc,
            "ADMINISTRATIVE_INFORMATION_CONTRACT_AWARD",
            "FILE_REFERENCE_NUMBER",
            "P"
          ).map(FileReference(_)),
          previousPublication = parsePreviousPublication(proc)
        ),
        awards = awards.map(parseAwardNode).toList,
        complementaryInfo = parseComplementaryAward(comp)
      )
    )

  // ── F04 ─────────────────────────────────────────────────────────────────────

  private def parseF04(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F04PeriodicIndicativeUtilities] =
    Right(
      F04PeriodicIndicativeUtilities(
        meta = meta,
        contractingEntity = parseContractingProfile(fd),
        activitiesOfEntity =
          opt(fd \\ "ACTIVITIES_OF_CONTRACTING_ENTITY", "P").map(RichText(_)),
        internetAddresses = parseInternetAddresses(fd),
        contractObjects =
          Nil, // TODO: map fd \ "OBJECT_CONTRACT_PERIODIC_UTILITIES"
        procedureAdmin = Nil,
        complementaryInfo = F04ComplementaryInfo(
          proceduresForAppeal = parseProceduresForAppeal(fd),
          additionalInfo =
            opt(fd \\ "ADDITIONAL_INFORMATION", "P").map(RichText(_)),
          dispatchDate = structDateAt(fd, "NOTICE_DISPATCH_DATE")
        )
      )
    )

  // ── F05 ─────────────────────────────────────────────────────────────────────

  private def parseF05(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F05ContractUtilities] =
    val auth = fd \ "CONTRACTING_ENTITY_CONTRACT_UTILITIES"
    val obj =
      fd \ "OBJECT_CONTRACT_CONTRACT_UTILITIES" \ "DESCRIPTION_CONTRACT_UTILITIES"
    val lefti = fd \ "LEFTI_CONTRACT"
    val proc = fd \ "PROCEDURE_DEFINITION_CONTRACT_NOTICE_UTILITIES"
    val comp = fd \ "COMPLEMENTARY_INFORMATION_CONTRACT_NOTICE"
    Right(
      F05ContractUtilities(
        meta = meta,
        contractingEntity = parseContractingProfile(auth),
        activitiesOfEntity =
          opt(auth \\ "ACTIVITIES_OF_CONTRACTING_ENTITY", "P").map(RichText(_)),
        internetAddresses = parseInternetAddresses(auth),
        contractObject = F05ContractObject(
          title = opt(obj, "TITLE_CONTRACT", "P").map(RichText(_)),
          contractType =
            ContractType.from(atr(obj \\ "TYPE_CONTRACT", "VALUE")),
          suppliesType =
            SuppliesType.from(atr(obj \\ "TYPE_SUPPLIES_CONTRACT", "VALUE")),
          location = parseLocationNuts(obj),
          description = RichText(txt(obj, "SHORT_CONTRACT_DESCRIPTION", "P")),
          cpv = {
            val code = atr(obj \\ "CPV_MAIN" \ "CPV_CODE", "CODE")
            if code.nonEmpty then Some(CpvCodes(CpvCode.unsafe(code), Nil))
            else None
          },
          lots = Nil,
          estimatedValue = parseEstimatedValue(obj),
          frameworkAgreement = {
            if (obj \\ "ESTABLISHMENT_FRAMEWORK_AGREEMENT").nonEmpty then
              Some(true)
            else None
          }
        ),
        lefti = F05Lefti(
          personalSituation =
            opt(lefti, "PERSONAL_SITUATION_OF_OPERATORS", "P").map(RichText(_)),
          economicCapacity =
            opt(lefti, "ECONOMIC_FINANCIAL_CAPACITY", "P").map(RichText(_)),
          technicalCapacity =
            opt(lefti, "TECHNICAL_CAPACITY_LEFTI", "P").map(RichText(_))
        ),
        procedure = F05Procedure(
          procedureType = {
            val v = atr(proc \\ "TYPE_OF_PROCEDURE_FOR_CONTRACT", "VALUE", "TYPE_OF_PROCEDURE_DETAIL")
            if v.nonEmpty then Some(RichText(v)) else None
          },
          awardCriteria = None,
          electronicAuction = None,
          fileReference =
            opt(proc \\ "FILE_REFERENCE_NUMBER", "P").map(FileReference(_)),
          receiptDeadline =
            opt(proc \\ "RECEIPT_LIMIT_DATE").map(TedDateTime.unsafe)
        ),
        complementaryInfo = parseComplementaryNotice(comp)
      )
    )

  // ── F06 ─────────────────────────────────────────────────────────────────────

  private def parseF06(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F06ContractAwardUtilities] =
    val auth = fd \ "CONTRACTING_ENTITY_CONTRACT_AWARD_UTILITIES"
    val obj = fd \ "OBJECT_CONTRACT_AWARD_UTILITIES"
    val proc = fd \ "PROCEDURES_CONTRACT_AWARD_UTILITIES"
    val awards =
      fd \ "AWARD_CONTRACT_CONTRACT_AWARD_UTILITIES" \ "AWARD_AND_CONTRACT_VALUE"
    val comp = fd \ "COMPLEMENTARY_INFORMATION_CONTRACT_AWARD_UTILITIES"
    Right(
      F06ContractAwardUtilities(
        meta = meta,
        contractingEntity = parseContractingProfile(auth),
        activitiesOfEntity =
          opt(auth \\ "ACTIVITIES_OF_CONTRACTING_ENTITY", "P").map(RichText(_)),
        contractObject = F06ContractObject(
          title = opt(obj, "TITLE_CONTRACT", "P").map(RichText(_)),
          contractType =
            ContractType.from(atr(obj \\ "TYPE_CONTRACT", "VALUE")),
          suppliesType =
            SuppliesType.from(atr(obj \\ "TYPE_SUPPLIES_CONTRACT", "VALUE")),
          location = parseLocationNuts(obj),
          description = opt(obj, "SHORT_DESCRIPTION", "P").map(RichText(_)),
          cpv = {
            val code = atr(obj \\ "CPV_MAIN" \ "CPV_CODE", "CODE")
            if code.nonEmpty then Some(CpvCodes(CpvCode.unsafe(code), Nil))
            else None
          },
          totalFinalValue = parseTotalFinalValue(obj)
        ),
        procedures = F06Procedures(
          procedureType = detectProcedureType(proc),
          awardCriteria = None,
          electronicAuction = {
            if (proc \\ "F06_ELECTRONIC_AUCTION").nonEmpty then Some(true)
            else if (proc \\ "NO_ELECTRONIC_AUCTION").nonEmpty then Some(false)
            else None
          },
          fileReference =
            opt(proc \\ "FILE_REFERENCE_NUMBER", "P").map(FileReference(_)),
          previousPub = parsePreviousPublication(proc)
        ),
        awards = awards.map { a =>
          val n = NodeSeq.fromSeq(Seq(a))
          AwardAndContractValue(
            item = opt(n, "ITEM").map(RichText(_)),
            contractNo = opt(n, "CONTRACT_NO").map(RichText(_)),
            lotNumbers =
              (n \ "LOT_NUMBER").map(e => RichText(e.text.trim)).toList,
            contractTitle =
              opt(deep(n, "TITLE_CONTRACT"), "P").map(RichText(_)),
            awardDate = structDateAt(n, "DATE_OF_CONTRACT_AWARD"),
            offersReceived =
              opt(n, "OFFERS_RECEIVED_NUMBER").flatMap(_.toIntOption),
            winner = {
              val eona = n \ "ECONOMIC_OPERATOR_NAME_ADDRESS"
              if eona.nonEmpty then Some(parseContactData(eona)) else None
            },
            valueInfo = None,
            pricePaid = {
              val pp = (n \\ "PRICE_PAID").headOption
              pp.flatMap { el =>
                MonetaryAmount.fromString(el.text.trim).toOption.map { amt =>
                  ValueCost(amt, Currency.from(el \@ "CURRENCY"))
                }
              }
            },
            subcontracted = {
              val sub = (n \\ "MORE_INFORMATION_TO_SUB_CONTRACTED").headOption
              sub.map(s => (s \ "CONTRACT_LIKELY_SUB_CONTRACTED").nonEmpty)
            }
          )
        }.toList,
        complementaryInfo = parseComplementaryAward(comp)
      )
    )

  // ── F07-F13 stubs ────────────────────────────────────────────────────────────
  // These forms share the same infrastructure; implement the sub-sections
  // following the same pattern as F01-F06 above.

  private def parseF07(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F07QualificationSystemUtilities] =
    Right(
      F07QualificationSystemUtilities(
        meta,
        parseContractingProfile(fd),
        None,
        Nil,
        F07Lefti(None, None),
        F07Procedures(None, None),
        parseComplementaryNotice(fd)
      )
    )

  private def parseF08(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F08BuyerProfile] =
    Right(
      F08BuyerProfile(
        meta,
        parseContractingProfile(fd),
        None,
        Nil,
        F08ComplementaryInfo(
          opt(fd \\ "ADDITIONAL_INFORMATION", "P").map(RichText(_)),
          structDateAt(fd, "NOTICE_DISPATCH_DATE")
        )
      )
    )

  private def parseF09(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F09SimplifiedContract] =
    Right(
      F09SimplifiedContract(
        meta,
        RichText(""),
        parseContractingProfile(fd),
        None,
        F09ContractObject(
          None,
          None,
          RichText(txt(fd \\ "SHORT_CONTRACT_DESCRIPTION", "P")),
          None,
          None
        ),
        F09Procedures(None, None, None),
        parseComplementaryNotice(fd)
      )
    )

  private def parseF10(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F10Concession] =
    Right(
      F10Concession(
        meta,
        parseContractingProfile(fd),
        None,
        F10ContractObject(
          None,
          RichText(txt(fd \\ "SHORT_CONTRACT_DESCRIPTION", "P")),
          None,
          None
        ),
        F10Lefti(None),
        F10Procedures(None, None, None),
        parseComplementaryNotice(fd)
      )
    )

  private def parseF11(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F11ContractConcessionaire] =
    Right(
      F11ContractConcessionaire(
        meta,
        parseContractingProfile(fd),
        F11ContractObject(
          None,
          RichText(txt(fd \\ "SHORT_CONTRACT_DESCRIPTION", "P")),
          None,
          None,
          None
        ),
        F11Lefti(None),
        F11Procedures(None, None),
        parseComplementaryNotice(fd)
      )
    )

  private def parseF12(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F12DesignContest] =
    Right(
      F12DesignContest(
        meta,
        RichText(""),
        parseContractingProfile(fd),
        None,
        F12ContestObject(
          None,
          RichText(txt(fd \\ "SHORT_CONTRACT_DESCRIPTION", "P")),
          None,
          None,
          None,
          None,
          None,
          None
        ),
        F12Lefti(None),
        F12Procedures(None, None),
        parseComplementaryNotice(fd)
      )
    )

  private def parseF13(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F13ResultDesignContest] =
    Right(
      F13ResultDesignContest(
        meta,
        RichText(""),
        parseContractingProfile(fd),
        None,
        F13ContestObject(None, None, None),
        F13Procedures(None, None, None),
        F13Results(false, Nil, None),
        parseComplementaryAward(fd)
      )
    )

  // ── F14 ─────────────────────────────────────────────────────────────────────
  // F14 (corrigendum) has a distinct structure: AUTH_ENTITY_ICAR / OBJECT_ICAR /
  // PROCEDURES_ICAR / COMPLEMENTARY_ICAR  — see fd_additional_information_corrigendum.

  private def parseF14(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F14AdditionalInformationCorrigendum] =
    val auth = fd \ "AUTH_ENTITY_ICAR"
    val obj = fd \ "OBJECT_ICAR" \ "DESCRIPTION_ICAR"
    val procs = fd \ "PROCEDURES_ICAR"
    val comp = fd \ "COMPLEMENTARY_ICAR"
    Right(
      F14AdditionalInformationCorrigendum(
        meta = meta,
        contractingAuthority = parseContractingProfile(auth),
        typeAndActivities = None,
        originalNotice = F14OriginalNotice(
          noticeNumber = {
            val num = txt(
              procs,
              "ADMINISTRATIVE_INFORMATION",
              "NOTICE_PUBLICATION",
              "NOTICE_NUMBER_OJ"
            )
            NoticeNumber.from(num).toOption
          },
          publicationDate = {
            val raw = txt(
              procs,
              "ADMINISTRATIVE_INFORMATION",
              "NOTICE_PUBLICATION",
              "DATE_OJ"
            )
            if raw.nonEmpty then Some(TedDate.unsafe(raw)) else None
          }
        ),
        procedures = F14Procedures(
          changes =
            (procs \\ "CORRECTION_ADDITIONAL_INFO" \\ "INFORMATION_CORRECTED_ADDED" \\ "REPLACE").map {
              r =>
                val n = NodeSeq.fromSeq(Seq(r))
                CorrigendumChange(
                  section = opt(n, "WHERE").map(RichText(_)),
                  lotNumber = None,
                  oldValue = opt(n, "OLD_VALUE").map(RichText(_)),
                  newValue = opt(n, "NEW_VALUE").map(RichText(_))
                )
            }.toList,
          additionalInfo =
            opt(comp, "OTHER_ADDITIONAL_INFO", "P").map(RichText(_))
        ),
        complementaryInfo = F14ComplementaryInfo(
          additionalInfo =
            opt(comp, "OTHER_ADDITIONAL_INFO", "P").map(RichText(_)),
          dispatchDate = structDateAt(comp, "NOTICE_DISPATCH_DATE")
        )
      )
    )

  // ── F15 ─────────────────────────────────────────────────────────────────────

  private def parseF15(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F15VeatNotice] =
    val auth   = fd \ "CONTRACTING_AUTHORITY_VEAT"
    val obj    = fd \ "OBJECT_VEAT" \ "DESCRIPTION_VEAT"
    val proc   = fd \ "PROCEDURE_DEFINITION_VEAT"
    val awards = fd \ "AWARD_OF_CONTRACT_DEFENCE"
    val comp   = fd \ "COMPLEMENTARY_INFORMATION_VEAT"
    Right(
      F15VeatNotice(
        meta = meta,
        contractingAuthority = parseContractingProfile(auth),
        typeAndActivities = parseTypeAndActivities(
          auth \\ "TYPE_AND_ACTIVITIES_OR_CONTRACTING_ENTITY_AND_PURCHASING_ON_BEHALF"
        ),
        purchasingOnBehalf = parsePurchasingOnBehalf(auth),
        noticePublished = {
          val num = txt(
            proc \\ "PREVIOUS_PUBLICATION_INFORMATION_NOTICE_F15" \\ "NOTICE_NUMBER_OJ"
          )
          NoticeNumber.from(num).toOption
        },
        contractObject = F15ContractObject(
          title = opt(obj, "TITLE_CONTRACT", "P").map(RichText(_)),
          contractType =
            ContractType.from(atr(obj \\ "TYPE_CONTRACT", "VALUE")),
          description = RichText(txt(obj, "SHORT_CONTRACT_DESCRIPTION", "P")),
          cpv = {
            val code = atr(obj \\ "CPV_MAIN" \ "CPV_CODE", "CODE")
            if code.nonEmpty then Some(CpvCodes(CpvCode.unsafe(code), Nil))
            else None
          },
          location = parseLocationNuts(obj),
          totalValue = parseTotalFinalValue(fd \ "OBJECT_VEAT")
        ),
        procedure = F15Procedure(
          procedureType = detectProcedureType(proc),
          justification =
            opt(proc \\ "REASON_CONTRACT_LAWFUL", "P").map(RichText(_)),
          fileReference =
            opt(proc \\ "FILE_REFERENCE_NUMBER", "P").map(FileReference(_)),
          previousPublication = parsePreviousPublication(proc)
        ),
        awards = awards.map(parseAwardNode).toList,
        complementaryInfo = parseComplementaryAward(comp)
      )
    )

  // ── F16 ─────────────────────────────────────────────────────────────────────

  private def parseF16(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F16PriorInformationDefence] =
    Right(
      F16PriorInformationDefence(
        meta = meta,
        contractingAuthority = parseContractingProfile(fd),
        typeAndActivities = parseTypeAndActivities(fd),
        contractObjects = Nil,
        lefti = None,
        complementaryInfo = F01ComplementaryInfo(
          None,
          None,
          None,
          structDateAt(fd, "NOTICE_DISPATCH_DATE")
        )
      )
    )

  // ── F17 ─────────────────────────────────────────────────────────────────────

  private def parseF17(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F17ContractDefence] =
    val auth = fd \ "CONTRACTING_AUTHORITY_INFORMATION_DEFENCE"
    val obj = fd \ "OBJECT_CONTRACT_DEFENCE" \ "DESCRIPTION_CONTRACT_DEFENCE"
    val comp = fd \ "COMPLEMENTARY_INFORMATION_CONTRACT_NOTICE"
    Right(
      F17ContractDefence(
        meta = meta,
        contractingAuthority = parseContractingProfile(auth),
        typeAndActivities = parseTypeAndActivities(auth),
        contractObject = F17ContractObject(
          title = opt(obj, "TITLE_CONTRACT", "P").map(RichText(_)),
          contractType =
            ContractType.from(atr(obj \\ "TYPE_CONTRACT_DEFENCE", "VALUE")),
          description = RichText(txt(obj, "SHORT_CONTRACT_DESCRIPTION", "P")),
          cpv = {
            val code = atr(obj \\ "CPV_MAIN" \ "CPV_CODE", "CODE")
            if code.nonEmpty then Some(CpvCodes(CpvCode.unsafe(code), Nil))
            else None
          },
          location = parseLocationNuts(obj)
        ),
        lefti = F17Lefti(None, None, None, None),
        procedure = F17Procedure(detectProcedureType(fd), None, None, None),
        complementaryInfo = parseComplementaryNotice(comp)
      )
    )

  // ── F18 ─────────────────────────────────────────────────────────────────────

  private def parseF18(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F18ContractAwardDefence] =
    val auth = fd \ "CONTRACTING_AUTHORITY_INFORMATION_CONTRACT_AWARD_DEFENCE"
    val obj = fd \ "OBJECT_CONTRACT_INFORMATION_CONTRACT_AWARD_NOTICE_DEFENCE"
    val proc = fd \ "PROCEDURE_DEFINITION_CONTRACT_AWARD_NOTICE_DEFENCE"
    val awards = fd \ "AWARD_OF_CONTRACT_DEFENCE"
    val comp = fd \ "COMPLEMENTARY_INFORMATION_CONTRACT_AWARD"
    Right(
      F18ContractAwardDefence(
        meta = meta,
        contractingAuthority = parseContractingProfile(auth),
        typeAndActivities = parseTypeAndActivities(auth),
        contractObject = F18ContractObject(
          title = opt(obj \\ "TITLE_CONTRACT", "P").map(RichText(_)),
          contractType =
            ContractType.from(atr(obj \\ "TYPE_CONTRACT_DEFENCE", "VALUE")),
          description =
            opt(obj \\ "SHORT_CONTRACT_DESCRIPTION", "P").map(RichText(_)),
          cpv = {
            val code = atr(obj \\ "CPV_MAIN" \ "CPV_CODE", "CODE")
            if code.nonEmpty then Some(CpvCodes(CpvCode.unsafe(code), Nil))
            else None
          },
          location = parseLocationNuts(obj),
          totalValue = parseTotalFinalValue(obj)
        ),
        procedure = F18Procedure(
          procedureType = detectProcedureType(proc),
          electronicAuction = None,
          fileReference =
            opt(proc \\ "FILE_REFERENCE_NUMBER", "P").map(FileReference(_)),
          previousPublication = parsePreviousPublication(proc)
        ),
        awards = awards.map(parseAwardNode).toList,
        complementaryInfo = parseComplementaryAward(comp)
      )
    )

  // ── F19 ─────────────────────────────────────────────────────────────────────

  private def parseF19(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, F19ContractSubDefence] =
    Right(
      F19ContractSubDefence(
        meta = meta,
        contractingAuthority = parseContractingProfile(fd),
        typeAndActivities = parseTypeAndActivities(fd),
        contractObject = F19ContractObject(
          title = opt(fd \\ "TITLE_CONTRACT", "P").map(RichText(_)),
          description = RichText(txt(fd \\ "SHORT_CONTRACT_DESCRIPTION", "P")),
          cpv = None,
          location = None
        ),
        lefti = F19Lefti(None),
        procedure = F19Procedure(detectProcedureType(fd), None, None),
        complementaryInfo = parseComplementaryNotice(fd)
      )
    )

  // ── T01 ─────────────────────────────────────────────────────────────────────

  private def parseT01(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, T01PriorInformationMove] =
    val auth = fd \ "AUTHORITY_PRIOR_INFORMATION_MOVE"
    val obj = fd \ "OBJECT_WORKS_PRIOR_INFORMATION_MOVE" match
      case n if n.nonEmpty => n
      case _ => fd \ "OBJECT_SUPPLIES_SERVICES_PRIOR_INFORMATION_MOVE"
    val awards = fd \ "AWARD_CONTRACT_PI_MOVE"
    Right(
      T01PriorInformationMove(
        meta = meta,
        authority = parseContractingProfile(auth),
        typeAndActivities = parseTypeAndActivities(auth),
        contractObject = T01ContractObject(
          title = opt(obj \\ "TITLE_CONTRACT", "P").map(RichText(_)),
          contractType =
            ContractType.from(atr(obj \\ "TYPE_CONTRACT", "VALUE")),
          description =
            opt(obj \\ "SHORT_CONTRACT_DESCRIPTION", "P").map(RichText(_)),
          cpv = {
            val code = atr(obj \\ "CPV_MAIN" \ "CPV_CODE", "CODE")
            if code.nonEmpty then Some(CpvCodes(CpvCode.unsafe(code), Nil))
            else None
          },
          location = parseLocationNuts(obj),
          estimatedValue = parseEstimatedValue(obj)
        ),
        lefti = None,
        procedure = T01Procedure(None, None, None),
        awards = awards.map(parseAwardNode).toList,
        complementaryInfo = T01ComplementaryInfo(
          additionalInfo =
            opt(fd \\ "ADDITIONAL_INFORMATION", "P").map(RichText(_)),
          dispatchDate = structDateAt(fd, "NOTICE_DISPATCH_DATE")
        )
      )
    )

  // ── T02 ─────────────────────────────────────────────────────────────────────

  private def parseT02(
      fd: NodeSeq,
      meta: FormMeta
  ): Either[ParseError, T02ContractMove] =
    val auth = fd \ "CONTRACTING_AUTHORITY_INFORMATION_MOVE"
    val obj =
      fd \ "OBJECT_CONTRACT_INFORMATION_MOVE" \ "DESCRIPTION_CONTRACT_INFORMATION_MOVE"
    val awards = fd \ "AWARD_CONTRACT_MOVE"
    Right(
      T02ContractMove(
        meta = meta,
        authority = parseContractingProfile(auth),
        typeAndActivities = parseTypeAndActivities(auth),
        contractObject = T02ContractObject(
          title = opt(obj, "TITLE_CONTRACT", "P").map(RichText(_)),
          description =
            opt(obj, "SHORT_CONTRACT_DESCRIPTION", "P").map(RichText(_)),
          cpv = {
            val code = atr(obj \\ "CPV_MAIN" \ "CPV_CODE", "CODE")
            if code.nonEmpty then Some(CpvCodes(CpvCode.unsafe(code), Nil))
            else None
          },
          location = parseLocationNuts(obj)
        ),
        lefti = None,
        procedure = T02Procedure(
          None,
          None,
          None,
          parsePreviousPublication(fd \ "PROCEDURE_DEFINITION_CONTRACT_MOVE")
        ),
        awards = awards.map(parseAwardNode).toList,
        complementaryInfo = T01ComplementaryInfo(
          additionalInfo =
            opt(fd \\ "ADDITIONAL_INFORMATION", "P").map(RichText(_)),
          dispatchDate = structDateAt(fd, "NOTICE_DISPATCH_DATE")
        )
      )
    )

  // ─────────────────────────────────────────────────────────────────────────────
  //  8. Top-level entry point
  // ─────────────────────────────────────────────────────────────────────────────

  /** Parse one TED XML file into a Notice. Returns Left[ParseError] if the file
    * cannot be parsed as XML or a required envelope field is missing.
    */
  def parse(file: File): Either[ParseError, Notice] =
    val root: Either[ParseError, Elem] =
      Try(XML.loadFile(file)).toEither.left.map(e =>
        ParseError.XmlError(file.getName, e)
      )
    root.flatMap { r =>
      // Infer schema version from root namespace or VERSION attribute
      val schemaVersion = SchemaVersion.from(
        Option(r \@ "VERSION").filter(_.nonEmpty)
          .orElse(
            // Older notices carry VERSION on the form element, not on TED_EXPORT
            (r \ "FORM_SECTION").headOption.toList
              .flatMap(_.child.collect { case e: scala.xml.Elem => e })
              .flatMap(e => Option(e \@ "VERSION").filter(_.nonEmpty))
              .headOption
          )
          .getOrElse(r.namespace)
      )
      for
        docId <- req(r \@ "DOC_ID", "DOC_ID", "TED_EXPORT").map(
          DocumentId.unsafe
        )
        edition <- req(r \@ "EDITION", "EDITION", "TED_EXPORT").map(
          Edition.unsafe
        )
        technical <- parseTechnical(r)
        coded <- parseCodedDataSection(r)
        formSection <- parseFormSection(r)
      yield Notice(
        docId = docId,
        edition = edition,
        schemaVersion = schemaVersion,
        technicalSection = technical,
        linkSection = parseLinks(r),
        sender = parseSender(r),
        codedDataSection = coded,
        translationSection = parseTranslationSection(r),
        formSection = formSection
      )
    }
