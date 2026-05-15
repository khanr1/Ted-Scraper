package io.github.khanr1.tedscraper.eforms

// Iron 3.0.2 runtime constraint resolution (refineEither/refineUnsafe) is broken
// for compound constraints like Length[Greater[0]] due to ambiguous given instances.
// Following the existing project pattern (CpvCode, CountryCode, etc.): plain opaque
// types with manual validation.  The Iron library remains available for future use.

// ── ID / text / url / phone / email opaque types ──
// fields.json type="id", type="text", type="url", type="phone", type="email"

opaque type NoticeId          = String
opaque type SdkVersion        = String
opaque type ContractFolderId  = String
opaque type NoticeVersionId   = String
opaque type NoticeSubtypeCode = String
opaque type LotId             = String
opaque type ResultId          = String
opaque type ContractId        = String
opaque type TenderId          = String
opaque type TenderingPartyId  = String
opaque type OrganizationId    = String
opaque type PlainText         = String
opaque type CodeValue         = String
opaque type UrlString         = String

private def nonEmpty(s: String, tag: String): Either[String, String] =
  if s.nonEmpty then Right(s) else Left(s"$tag must be non-empty")

object NoticeId:
  def from(s: String): Either[String, NoticeId]    = nonEmpty(s, "NoticeId")
  def unsafe(s: String): NoticeId                  = s
  extension (v: NoticeId) def value: String        = v

object SdkVersion:
  def from(s: String): Either[String, SdkVersion]  = nonEmpty(s, "SdkVersion")
  def unsafe(s: String): SdkVersion                = s
  extension (v: SdkVersion) def value: String      = v

object ContractFolderId:
  def from(s: String): Either[String, ContractFolderId] = nonEmpty(s, "ContractFolderId")
  def unsafe(s: String): ContractFolderId               = s
  extension (v: ContractFolderId) def value: String     = v

object NoticeVersionId:
  def from(s: String): Either[String, NoticeVersionId] = nonEmpty(s, "NoticeVersionId")
  def unsafe(s: String): NoticeVersionId               = s
  extension (v: NoticeVersionId) def value: String     = v

object NoticeSubtypeCode:
  def from(s: String): Either[String, NoticeSubtypeCode] = nonEmpty(s, "NoticeSubtypeCode")
  def unsafe(s: String): NoticeSubtypeCode               = s
  extension (v: NoticeSubtypeCode) def value: String     = v

object LotId:
  def from(s: String): Either[String, LotId]       = nonEmpty(s, "LotId")
  def unsafe(s: String): LotId                     = s
  extension (v: LotId) def value: String           = v

object ResultId:
  def from(s: String): Either[String, ResultId]    = nonEmpty(s, "ResultId")
  def unsafe(s: String): ResultId                  = s
  extension (v: ResultId) def value: String        = v

object ContractId:
  def from(s: String): Either[String, ContractId]  = nonEmpty(s, "ContractId")
  def unsafe(s: String): ContractId                = s
  extension (v: ContractId) def value: String      = v

object TenderId:
  def from(s: String): Either[String, TenderId]    = nonEmpty(s, "TenderId")
  def unsafe(s: String): TenderId                  = s
  extension (v: TenderId) def value: String        = v

object TenderingPartyId:
  def from(s: String): Either[String, TenderingPartyId] = nonEmpty(s, "TenderingPartyId")
  def unsafe(s: String): TenderingPartyId               = s
  extension (v: TenderingPartyId) def value: String     = v

object OrganizationId:
  def from(s: String): Either[String, OrganizationId] = nonEmpty(s, "OrganizationId")
  def unsafe(s: String): OrganizationId               = s
  extension (v: OrganizationId) def value: String     = v

object PlainText:
  def from(s: String): Either[String, PlainText]   = nonEmpty(s, "PlainText")
  def unsafe(s: String): PlainText                 = s
  extension (v: PlainText) def value: String       = v

object CodeValue:
  def from(s: String): Either[String, CodeValue]   = nonEmpty(s, "CodeValue")
  def unsafe(s: String): CodeValue                 = s
  extension (v: CodeValue) def value: String       = v

object UrlString:
  def from(s: String): Either[String, UrlString]   = nonEmpty(s, "UrlString")
  def unsafe(s: String): UrlString                 = s
  extension (v: UrlString) def value: String       = v

// ── Date / time — fields.json type="date", type="time" ──

opaque type DateString = String
opaque type TimeString = String

object DateString:
  private val R = raw"\d{4}-\d{2}-\d{2}.*".r
  def from(s: String): Either[String, DateString]  =
    if R.matches(s) then Right(s) else Left(s"Invalid date: '$s'")
  def unsafe(s: String): DateString                = s
  extension (v: DateString) def value: String      = v

object TimeString:
  private val R = raw"\d{2}:\d{2}:\d{2}.*".r
  def from(s: String): Either[String, TimeString]  =
    if R.matches(s) then Right(s) else Left(s"Invalid time: '$s'")
  def unsafe(s: String): TimeString                = s
  extension (v: TimeString) def value: String      = v

// ── Code types with structural regex ──

opaque type CpvCode  = String
opaque type NutsCode = String

object CpvCode:
  private val R = raw"\d{8}".r
  def from(s: String): Either[String, CpvCode]    =
    if R.matches(s) then Right(s) else Left(s"Invalid CPV code: '$s'")
  def unsafe(s: String): CpvCode                  = s
  extension (v: CpvCode) def value: String        = v

object NutsCode:
  private val R = raw"[A-Z0-9]{2,5}".r
  def from(s: String): Either[String, NutsCode]   =
    if R.matches(s) then Right(s) else Left(s"Invalid NUTS code: '$s'")
  def unsafe(s: String): NutsCode                 = s
  extension (v: NutsCode) def value: String       = v

// ── Amount — fields.json type="amount" ──

opaque type Amount = Double

object Amount:
  def from(d: Double): Either[String, Amount]     =
    if d > 0 then Right(d) else Left(s"Amount must be positive, got: $d")
  def unsafe(d: Double): Amount                   = d
  extension (v: Amount) def value: Double         = v

// ── Scala 3 enums for finite codelists ──

/** BT-11-Procedure-Buyer — codelist: buyer-legal-type */
enum BuyerLegalType:
  case BodyPl, BodyPlCga, BodyPlCla, BodyPlEu, Def, Grp, Int, Natl,
       OrgSub, Ra, RaAcr, RaCga, Reg, Spb, Unknown

object BuyerLegalType:
  private val mapping: Map[String, BuyerLegalType] = Map(
    "body-pl"     -> BodyPl,
    "body-pl-cga" -> BodyPlCga,
    "body-pl-cla" -> BodyPlCla,
    "body-pl-eu"  -> BodyPlEu,
    "def"         -> Def,
    "grp"         -> Grp,
    "int"         -> Int,
    "natl"        -> Natl,
    "org-sub"     -> OrgSub,
    "ra"          -> Ra,
    "ra-acr"      -> RaAcr,
    "ra-cga"      -> RaCga,
    "reg"         -> Reg,
    "spb"         -> Spb
  )
  def fromString(s: String): BuyerLegalType = mapping.getOrElse(s.toLowerCase, Unknown)

/** BT-23-Lot / BT-23-Procedure — codelist: contract-nature */
enum ContractNature:
  case Services, Supplies, Works, Unknown

object ContractNature:
  private val mapping: Map[String, ContractNature] = Map(
    "services" -> Services,
    "supplies" -> Supplies,
    "works"    -> Works
  )
  def fromString(s: String): ContractNature = mapping.getOrElse(s.toLowerCase, Unknown)

/** BT-142-LotResult — codelist: winner-selection-status */
enum WinnerSelectionStatus:
  case SelecW, NoAwa, CancelProcNoAwa, Unknown

object WinnerSelectionStatus:
  private val mapping: Map[String, WinnerSelectionStatus] = Map(
    "selec-w"            -> SelecW,
    "no-awa"             -> NoAwa,
    "cancel-proc-no-awa" -> CancelProcNoAwa
  )
  def fromString(s: String): WinnerSelectionStatus = mapping.getOrElse(s.toLowerCase, Unknown)

/** Document type inferred from root XML element label */
enum DocumentType:
  case Pin, Cn, Can, Unknown

/** Notice form type (for service-level filtering) */
enum NoticeFormType:
  case Pin, Cn, Can, Veat, Modification, Unknown
