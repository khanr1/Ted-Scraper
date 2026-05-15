package io.github.khanr1.tedscraper.eforms

// ── ND-Root → NoticeMetadata ──────────────────────────────────────────────────
// Fields: BT-701(noticeId), OPT-002(sdkVersion), BT-04(contractFolderId),
//         BT-05(a)(issueDate), BT-05(b)(issueTime), BT-02(noticeTypeCode),
//         BT-702(a)(language), BT-01(regulatoryDomain), BT-757(versionId)

final case class NoticeMetadata(
  noticeId: NoticeId,
  sdkVersion: SdkVersion,
  contractFolderId: Option[ContractFolderId],
  issueDate: Option[DateString],
  issueTime: Option[TimeString],
  noticeTypeCode: NoticeSubtypeCode,
  language: Option[PlainText],
  regulatoryDomain: Option[PlainText],
  versionId: Option[PlainText],
  noticePublicationId: Option[PlainText], // efac:Publication/efbc:NoticePublicationID[@schemeName='ojs-notice-id']
  gazetteId: Option[PlainText]            // efac:Publication/efbc:GazetteID[@schemeName='ojs-id']
)

// ── ND-ContractingParty / ND-Buyer → BuyerRef ────────────────────────────────
// Fields: OPT-300(organizationRef), BT-11(legalType), BT-508(buyerProfileUrl)

final case class BuyerRef(
  organizationRef: OrganizationId,
  legalType: Option[BuyerLegalType],
  buyerProfileUrl: Option[UrlString]
)

// ── ND-Company postal address ─────────────────────────────────────────────────

final case class CompanyAddress(
  street: Option[PlainText],
  city: Option[PlainText],
  postalCode: Option[PlainText],
  nuts: Option[NutsCode],
  country: Option[PlainText]
)

// ── ND-Company ────────────────────────────────────────────────────────────────
// Fields: OPT-200(organizationId), BT-500(name), BT-505(website),
//         BT-509(endpoint), contact phone/email

final case class Company(
  organizationId: OrganizationId,
  name: Option[PlainText],
  website: Option[UrlString],
  endpoint: Option[UrlString],
  address: Option[CompanyAddress],
  phone: Option[PlainText],
  email: Option[PlainText]
)

// ── ND-Organization ───────────────────────────────────────────────────────────
// Fields: BT-633(naturalPerson), BT-746(listedOnMarket),
//         OPP-050(isGroupLead), OPP-051(isAwardingCPB), OPP-052(isAcquiringCPB)

final case class Organization(
  company: Company,
  naturalPerson: Option[Boolean],
  listedOnMarket: Option[Boolean],
  isGroupLead: Option[Boolean],
  isAwardingCPB: Option[Boolean],
  isAcquiringCPB: Option[Boolean]
)

// ── ND-LotProcurementScope → LotScope ─────────────────────────────────────────
// Fields: BT-21(lotTitle), BT-24(lotDescription), BT-23(contractNature),
//         BT-22(mainCpvCode), BT-300(additionalInfo), NUTS from RealizedLocation

final case class LotScope(
  lotTitle: Option[PlainText],
  lotDescription: Option[PlainText],
  contractNature: Option[ContractNature],
  mainCpvCode: Option[CpvCode],
  additionalInfo: Option[PlainText],
  nutsCode: List[NutsCode]
)

// ── ND-LotTenderingProcess → LotProcess ───────────────────────────────────────

final case class LotProcess(
  submissionDeadline: Option[DateString]
)

// ── ND-LotTenderingTerms → LotTerms ───────────────────────────────────────────

final case class LotTerms()

// ── ND-Lot ────────────────────────────────────────────────────────────────────
// BT-137-Lot: cbc:ID[@schemeName='Lot']

final case class Lot(
  lotId: LotId,
  scope: LotScope,
  process: Option[LotProcess],
  terms: Option[LotTerms]
)

// ── ND-LotsGroup ──────────────────────────────────────────────────────────────
// cbc:ID[@schemeName='LotsGroup']

final case class LotsGroup(
  groupId: LotId
)

// ── ND-Part ───────────────────────────────────────────────────────────────────
// cbc:ID[@schemeName='Part']

final case class Part(
  partId: LotId,
  title: Option[PlainText],
  description: Option[PlainText]
)

// ── ND-TenderingParty ─────────────────────────────────────────────────────────
// Fields: OPT-210(tenderingPartyId), OPT-211(name)
// ND-Tenderer: tendererOrgRefs from efac:Tenderer/cbc:ID

final case class TenderingParty(
  tenderingPartyId: TenderingPartyId,
  name: Option[PlainText],
  tendererOrgRefs: List[OrganizationId]
)

// ── ND-SettledContract ────────────────────────────────────────────────────────
// Fields: OPT-316(contractId), BT-721(title), BT-145(issueDate),
//         BT-1451(awardDate), BT-150(contractReference), tenderRef,
//         cac:SignatoryParty → signatoryOrgRefs

final case class SettledContract(
  contractId: ContractId,
  title: Option[PlainText],
  issueDate: Option[DateString],
  awardDate: Option[DateString],
  contractReference: Option[PlainText],
  tenderRef: Option[TenderId],
  signatoryOrgRefs: List[OrganizationId]
)

// ── ND-LotTender ─────────────────────────────────────────────────────────────
// Inside efac:NoticeResult/efac:LotTender

final case class LotTender(
  tenderId: TenderId,
  payableAmount: Option[Amount],
  currency: Option[PlainText],
  rankCode: Option[PlainText],
  tenderingPartyRef: Option[TenderingPartyId],
  lotRef: Option[LotId]
)

// ── ND-LotResult ──────────────────────────────────────────────────────────────
// Fields: OPT-322(resultId), BT-142(tenderResultCode),
//         BT-710(lowerTenderAmount), BT-711(higherTenderAmount),
//         BT-13713(lotRef)

final case class LotResult(
  resultId: ResultId,
  tenderResultCode: WinnerSelectionStatus,
  lowerTenderAmount: Option[Amount],
  higherTenderAmount: Option[Amount],
  tenderRef: Option[TenderId],
  contractRef: Option[ContractId],
  lotRef: Option[LotId],
  offersReceived: Option[Int]  // BT-759-LotResult: StatisticsNumeric for received submissions
)

// ── ND-NoticeResult ───────────────────────────────────────────────────────────
// BT-161(totalAmount), aggregates all repeatable result sub-nodes

final case class NoticeResult(
  totalAmount: Option[Amount],
  currency: Option[PlainText],
  lotResults: List[LotResult],
  tenders: List[LotTender],
  settledContracts: List[SettledContract],
  tenderingParties: List[TenderingParty]
)

// ── ND-Root aggregated notice ─────────────────────────────────────────────────

final case class Notice(
  metadata: NoticeMetadata,
  buyers: List[BuyerRef],
  organizations: List[Organization],
  lots: List[Lot],
  lotsGroups: List[LotsGroup],
  parts: List[Part],
  noticeResult: Option[NoticeResult],
  procedureScope: Option[LotScope],     // ND-ProcedureProcurementScope: root-level cac:ProcurementProject
  procedureCode: Option[PlainText],     // BT-105: cac:TenderingProcess/cbc:ProcedureCode
  procedureJustification: Option[PlainText] // BT-135: ProcessReason text (direct-award-justification), or code if text absent
)

// ── Sealed notice discriminator ───────────────────────────────────────────────

sealed trait NoticeForm:
  def noticeSubtype: NoticeSubtypeCode
  def data: Notice

final case class PinNotice(noticeSubtype: NoticeSubtypeCode, data: Notice) extends NoticeForm
final case class CnNotice (noticeSubtype: NoticeSubtypeCode, data: Notice) extends NoticeForm
final case class CanNotice(noticeSubtype: NoticeSubtypeCode, data: Notice) extends NoticeForm

final case class UnknownNotice(rawSubtype: String, data: Notice) extends NoticeForm:
  def noticeSubtype: NoticeSubtypeCode = NoticeSubtypeCode.unsafe(rawSubtype)
