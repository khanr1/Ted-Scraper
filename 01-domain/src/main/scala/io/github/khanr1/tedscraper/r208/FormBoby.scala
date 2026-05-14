package io.github.khanr1.tedscraper.r208

import types.*

// ── F01 ───────────────────────────────────────────────────────────────────────

case class LotPriorInformation(
  lotNumber:      Option[RichText],
  lotTitle:       Option[RichText],
  lotDescription: Option[RichText],
  cpv:            Option[CpvCodes],
  natureQtyScope: Option[RichText],
  scheduledDate:  Option[RichText],
  additionalInfo: Option[RichText]
)

case class F01PriorInformation(
  meta:                 FormMeta,
  contractingAuthority: ContractingProfile,
  typeAndActivities:    Option[TypeAndActivities],
  purchasingOnBehalf:   Option[PurchasingOnBehalf],
  contractObject:       F01ContractObject,
  lefti:                Option[F01Lefti],
  complementaryInfo:    F01ComplementaryInfo
)

case class F01ContractObject(
  contractType:   Option[ContractType],
  description:    Option[RichText],
  cpv:            Option[CpvCodes],
  lots:           List[LotPriorInformation],
  estimatedValue: Option[FormContractValue]
)

case class F01Lefti(
  personalSituation: Option[RichText],
  economicCapacity:  Option[RichText],
  technicalCapacity: Option[RichText]
)

case class F01ComplementaryInfo(
  relatesToEuProject:             Option[Boolean],
  additionalInfo:                 Option[RichText],
  informationRegulatoryFramework: Option[RichText],
  dispatchDate:                   Option[StructuredDate]
)

// ── F02 ───────────────────────────────────────────────────────────────────────

case class AwardCriterion(name: RichText, weighting: Option[RichText])

case class AwardCriteriaDetail(
  lowestPrice: Boolean,
  criteria:    List[AwardCriterion]
)

case class F02ContractNotice(
  meta:                 FormMeta,
  contractingAuthority: ContractingProfile,
  internetAddresses:    Option[InternetAddresses],
  furtherInfo:          Option[ContactData],
  specsAndDocuments:    Option[ContactData],
  tendersTo:            Option[ContactData],
  typeAndActivities:    Option[TypeAndActivities],
  purchasingOnBehalf:   Option[PurchasingOnBehalf],
  contractObject:       F02ContractObject,
  lefti:                F02Lefti,
  procedure:            F02Procedure,
  complementaryInfo:    ComplementaryInfoNotice
)

case class F02ContractObject(
  title:              Option[RichText],
  contractType:       Option[ContractType],
  suppliesType:       Option[SuppliesType],
  location:           Option[LocationNuts],
  noticeInvolves:     Option[RichText],
  description:        RichText,
  cpv:                Option[CpvCodes],
  contractCoveredGpa: Option[Boolean],
  totalQuantityScope: Option[RichText],
  lots:               List[F02Lot],
  estimatedValue:     Option[FormContractValue],
  options:            Option[RichText],
  recurrentContract:  Option[Boolean]
)

case class F02Lot(
  lotNumber:      Option[RichText],
  lotTitle:       Option[RichText],
  description:    Option[RichText],
  cpv:            Option[CpvCodes],
  estimatedValue: Option[FormContractValue]
)

case class F02Lefti(
  personalSituation:  Option[RichText],
  economicCapacity:   Option[RichText],
  technicalCapacity:  Option[RichText],
  contractConditions: Option[RichText],
  reservedContracts:  Option[Boolean]
)

case class F02Procedure(
  procedureType:         Option[ProcedureType],
  awardCriteria:         Option[AwardCriteriaDetail],
  electronicAuction:     Option[Boolean],
  fileReference:         Option[FileReference],
  previousPublication:   Option[PreviousPublication],
  receiptDeadline:       Option[TedDateTime],
  participateDeadline:   Option[TedDateTime],
  minimumTimeMainTender: Option[RichText],
  openingConditions:     Option[RichText]
)

// ── F03 ───────────────────────────────────────────────────────────────────────

case class F03ContractAward(
  meta:                 FormMeta,
  contractingAuthority: ContractingProfile,
  internetAddresses:    Option[InternetAddresses],
  typeAndActivities:    Option[TypeAndActivities],
  purchasingOnBehalf:   Option[PurchasingOnBehalf],
  contractObject:       F03ContractObject,
  procedure:            F03Procedure,
  awards:               List[AwardOfContract],
  complementaryInfo:    ComplementaryInfoAward
)

case class F03ContractObject(
  title:              Option[RichText],
  contractType:       Option[ContractType],
  suppliesType:       Option[SuppliesType],
  location:           Option[LocationNuts],
  noticeInvolves:     Option[RichText],
  description:        RichText,
  cpv:                Option[CpvCodes],
  contractCoveredGpa: Option[Boolean],
  totalFinalValue:    Option[TotalFinalValue]
)

case class F03Procedure(
  procedureType:       Option[ProcedureType],
  electronicAuction:   Option[Boolean],
  fileReference:       Option[FileReference],
  previousPublication: Option[PreviousPublication]
)

// ── F04 ───────────────────────────────────────────────────────────────────────

case class F04PeriodicIndicativeUtilities(
  meta:              FormMeta,
  contractingEntity: ContractingProfile,
  activitiesOfEntity: Option[RichText],
  internetAddresses: Option[InternetAddresses],
  contractObjects:   List[F04ContractObject],
  procedureAdmin:    List[F04ProcedureAdmin],
  complementaryInfo: F04ComplementaryInfo
)

case class F04ContractObject(
  description:    Option[RichText],
  cpv:            Option[CpvCodes],
  location:       Option[LocationNuts],
  estimatedValue: Option[FormContractValue],
  lots:           List[F04Lot]
)

case class F04Lot(
  lotNumber:      Option[RichText],
  description:    Option[RichText],
  estimatedValue: Option[FormContractValue]
)

case class F04ProcedureAdmin(
  procedureType:  Option[RichText],
  awardCriteria:  Option[AwardCriteriaDetail],
  fileReference:  Option[FileReference],
  receiptDeadline: Option[TedDateTime]
)

case class F04ComplementaryInfo(
  proceduresForAppeal: Option[ProceduresForAppeal],
  additionalInfo:      Option[RichText],
  dispatchDate:        Option[StructuredDate]
)

// ── F05 ───────────────────────────────────────────────────────────────────────

case class F05ContractUtilities(
  meta:              FormMeta,
  contractingEntity: ContractingProfile,
  activitiesOfEntity: Option[RichText],
  internetAddresses: Option[InternetAddresses],
  contractObject:    F05ContractObject,
  lefti:             F05Lefti,
  procedure:         F05Procedure,
  complementaryInfo: ComplementaryInfoNotice
)

case class F05ContractObject(
  title:             Option[RichText],
  contractType:      Option[ContractType],
  suppliesType:      Option[SuppliesType],
  location:          Option[LocationNuts],
  description:       RichText,
  cpv:               Option[CpvCodes],
  lots:              List[F05Lot],
  estimatedValue:    Option[FormContractValue],
  frameworkAgreement: Option[Boolean]
)

case class F05Lot(
  lotNumber:   Option[RichText],
  description: Option[RichText],
  cpv:         Option[CpvCodes]
)

case class F05Lefti(
  personalSituation: Option[RichText],
  economicCapacity:  Option[RichText],
  technicalCapacity: Option[RichText]
)

case class F05Procedure(
  procedureType:     Option[RichText],
  awardCriteria:     Option[AwardCriteriaDetail],
  electronicAuction: Option[Boolean],
  fileReference:     Option[FileReference],
  receiptDeadline:   Option[TedDateTime]
)

// ── F06 ───────────────────────────────────────────────────────────────────────

case class F06ContractAwardUtilities(
  meta:              FormMeta,
  contractingEntity: ContractingProfile,
  activitiesOfEntity: Option[RichText],
  contractObject:    F06ContractObject,
  procedures:        F06Procedures,
  awards:            List[AwardAndContractValue],
  complementaryInfo: ComplementaryInfoAward
)

case class F06ContractObject(
  title:           Option[RichText],
  contractType:    Option[ContractType],
  suppliesType:    Option[SuppliesType],
  location:        Option[LocationNuts],
  description:     Option[RichText],
  cpv:             Option[CpvCodes],
  totalFinalValue: Option[TotalFinalValue]
)

case class F06Procedures(
  procedureType:     Option[ProcedureType],
  awardCriteria:     Option[AwardCriteriaDetail],
  electronicAuction: Option[Boolean],
  fileReference:     Option[FileReference],
  previousPub:       Option[PreviousPublication]
)

// ── F07 ───────────────────────────────────────────────────────────────────────

case class F07QualificationSystemUtilities(
  meta:                 FormMeta,
  contractingEntity:    ContractingProfile,
  activitiesOfEntity:   Option[RichText],
  qualificationObjects: List[F07QualificationObject],
  lefti:                F07Lefti,
  procedures:           F07Procedures,
  complementaryInfo:    ComplementaryInfoNotice
)

case class F07QualificationObject(
  cpv:         Option[CpvCodes],
  description: Option[RichText]
)

case class F07Lefti(
  qualificationCriteria: Option[RichText],
  standardsRequired:     Option[RichText]
)

case class F07Procedures(
  awardCriteria: Option[AwardCriteriaDetail],
  fileReference: Option[FileReference]
)

// ── F08 ───────────────────────────────────────────────────────────────────────

case class F08BuyerProfile(
  meta:                 FormMeta,
  contractingAuthority: ContractingProfile,
  internetAddresses:    Option[InternetAddresses],
  noticeObjects:        List[F08NoticeObject],
  complementaryInfo:    F08ComplementaryInfo
)

case class F08NoticeObject(
  title:       Option[RichText],
  description: Option[RichText]
)

case class F08ComplementaryInfo(
  additionalInfo: Option[RichText],
  dispatchDate:   Option[StructuredDate]
)

// ── F09 ───────────────────────────────────────────────────────────────────────

case class F09SimplifiedContract(
  meta:                 FormMeta,
  noticeCovered:        RichText,
  contractingAuthority: ContractingProfile,
  typeAndActivities:    Option[TypeAndActivities],
  contractObject:       F09ContractObject,
  procedures:           F09Procedures,
  complementaryInfo:    ComplementaryInfoNotice
)

case class F09ContractObject(
  title:        Option[RichText],
  contractType: Option[ContractType],
  description:  RichText,
  cpv:          Option[CpvCodes],
  location:     Option[LocationNuts]
)

case class F09Procedures(
  procedureType:   Option[RichText],
  awardCriteria:   Option[AwardCriteriaDetail],
  receiptDeadline: Option[TedDateTime]
)

// ── F10 ───────────────────────────────────────────────────────────────────────

case class F10Concession(
  meta:                 FormMeta,
  contractingAuthority: ContractingProfile,
  typeAndActivities:    Option[TypeAndActivities],
  contractObject:       F10ContractObject,
  lefti:                F10Lefti,
  procedures:           F10Procedures,
  complementaryInfo:    ComplementaryInfoNotice
)

case class F10ContractObject(
  title:       Option[RichText],
  description: RichText,
  cpv:         Option[CpvCodes],
  location:    Option[LocationNuts]
)

case class F10Lefti(conditions: Option[RichText])

case class F10Procedures(
  procedureType:      Option[RichText],
  awardCriteria:      Option[AwardCriteriaDetail],
  submissionDeadline: Option[TedDateTime]
)

// ── F11 ───────────────────────────────────────────────────────────────────────

case class F11ContractConcessionaire(
  meta:                     FormMeta,
  contractingConcessionaire: ContractingProfile,
  contractObject:           F11ContractObject,
  lefti:                    F11Lefti,
  procedures:               F11Procedures,
  complementaryInfo:        ComplementaryInfoNotice
)

case class F11ContractObject(
  title:          Option[RichText],
  description:    RichText,
  cpv:            Option[CpvCodes],
  location:       Option[LocationNuts],
  estimatedValue: Option[FormContractValue]
)

case class F11Lefti(conditions: Option[RichText])
case class F11Procedures(
  awardCriteria:      Option[AwardCriteriaDetail],
  submissionDeadline: Option[TedDateTime]
)

// ── F12 ───────────────────────────────────────────────────────────────────────

sealed trait ContestType
case object OpenContest       extends ContestType
case object RestrictedContest extends ContestType

case class F12DesignContest(
  meta:                 FormMeta,
  noticeCovered:        RichText,
  contractingAuthority: ContractingProfile,
  typeAndActivities:    Option[TypeAndActivities],
  contestObject:        F12ContestObject,
  lefti:                F12Lefti,
  procedures:           F12Procedures,
  complementaryInfo:    ComplementaryInfoNotice
)

case class F12ContestObject(
  title:             Option[RichText],
  description:       RichText,
  cpv:               Option[CpvCodes],
  location:          Option[LocationNuts],
  contestType:       Option[ContestType],
  estimatedValue:    Option[FormContractValue],
  prizesDescription: Option[RichText],
  nbParticipants:    Option[Int]
)

case class F12Lefti(qualificationCriteria: Option[RichText])
case class F12Procedures(
  participationDeadline: Option[TedDateTime],
  awardCriteria:         Option[RichText]
)

// ── F13 ───────────────────────────────────────────────────────────────────────

case class F13ResultDesignContest(
  meta:              FormMeta,
  noticeCovered:     RichText,
  contractingEntity: ContractingProfile,
  typeAndActivities: Option[TypeAndActivities],
  contestObject:     F13ContestObject,
  procedures:        F13Procedures,
  results:           F13Results,
  complementaryInfo: ComplementaryInfoAward
)

case class F13ContestObject(
  title:       Option[RichText],
  description: Option[RichText],
  cpv:         Option[CpvCodes]
)

case class F13Procedures(
  contestType:    Option[ContestType],
  nbParticipants: Option[Int],
  awardCriteria:  Option[RichText]
)

case class F13Results(
  prizeAwarded: Boolean,
  winners:      List[ContactData],
  prizeValue:   Option[FormContractValue]
)

// ── F14 ───────────────────────────────────────────────────────────────────────

case class CorrigendumChange(
  section:   Option[RichText],
  lotNumber: Option[RichText],
  oldValue:  Option[RichText],
  newValue:  Option[RichText]
)

case class F14AdditionalInformationCorrigendum(
  meta:                 FormMeta,
  contractingAuthority: ContractingProfile,
  typeAndActivities:    Option[TypeAndActivities],
  originalNotice:       F14OriginalNotice,
  procedures:           F14Procedures,
  complementaryInfo:    F14ComplementaryInfo
)

case class F14OriginalNotice(
  noticeNumber:    Option[NoticeNumber],
  publicationDate: Option[TedDate]
)

case class F14Procedures(
  changes:        List[CorrigendumChange],
  additionalInfo: Option[RichText]
)

case class F14ComplementaryInfo(
  additionalInfo: Option[RichText],
  dispatchDate:   Option[StructuredDate]
)

// ── F15 ───────────────────────────────────────────────────────────────────────

case class F15VeatNotice(
  meta:                 FormMeta,
  contractingAuthority: ContractingProfile,
  typeAndActivities:    Option[TypeAndActivities],
  purchasingOnBehalf:   Option[PurchasingOnBehalf],
  noticePublished:      Option[NoticeNumber],
  contractObject:       F15ContractObject,
  procedure:            F15Procedure,
  awards:               List[AwardOfContract],
  complementaryInfo:    ComplementaryInfoAward
)

case class F15ContractObject(
  title:        Option[RichText],
  contractType: Option[ContractType],
  description:  RichText,
  cpv:          Option[CpvCodes],
  location:     Option[LocationNuts],
  totalValue:   Option[TotalFinalValue]
)

case class F15Procedure(
  procedureType:       Option[ProcedureType],
  justification:       Option[RichText],
  fileReference:       Option[FileReference],
  previousPublication: Option[PreviousPublication]
)

// ── F16 ───────────────────────────────────────────────────────────────────────

case class F16PriorInformationDefence(
  meta:                 FormMeta,
  contractingAuthority: ContractingProfile,
  typeAndActivities:    Option[TypeAndActivities],
  contractObjects:      List[F16ContractObject],
  lefti:                Option[F01Lefti],
  complementaryInfo:    F01ComplementaryInfo
)

case class F16ContractObject(
  contractType:   Option[ContractType],
  description:    Option[RichText],
  cpv:            Option[CpvCodes],
  location:       Option[LocationNuts],
  estimatedValue: Option[FormContractValue]
)

// ── F17 ───────────────────────────────────────────────────────────────────────

case class F17ContractDefence(
  meta:                 FormMeta,
  contractingAuthority: ContractingProfile,
  typeAndActivities:    Option[TypeAndActivities],
  contractObject:       F17ContractObject,
  lefti:                F17Lefti,
  procedure:            F17Procedure,
  complementaryInfo:    ComplementaryInfoNotice
)

case class F17ContractObject(
  title:        Option[RichText],
  contractType: Option[ContractType],
  description:  RichText,
  cpv:          Option[CpvCodes],
  location:     Option[LocationNuts]
)

case class F17Lefti(
  personalSituation: Option[RichText],
  economicCapacity:  Option[RichText],
  technicalCapacity: Option[RichText],
  subContractInfo:   Option[RichText]
)

case class F17Procedure(
  procedureType:   Option[ProcedureType],
  awardCriteria:   Option[AwardCriteriaDetail],
  fileReference:   Option[FileReference],
  receiptDeadline: Option[TedDateTime]
)

// ── F18 ───────────────────────────────────────────────────────────────────────

case class F18ContractAwardDefence(
  meta:                 FormMeta,
  contractingAuthority: ContractingProfile,
  typeAndActivities:    Option[TypeAndActivities],
  contractObject:       F18ContractObject,
  procedure:            F18Procedure,
  awards:               List[AwardOfContract],
  complementaryInfo:    ComplementaryInfoAward
)

case class F18ContractObject(
  title:        Option[RichText],
  contractType: Option[ContractType],
  description:  Option[RichText],
  cpv:          Option[CpvCodes],
  location:     Option[LocationNuts],
  totalValue:   Option[TotalFinalValue]
)

case class F18Procedure(
  procedureType:       Option[ProcedureType],
  electronicAuction:   Option[Boolean],
  fileReference:       Option[FileReference],
  previousPublication: Option[PreviousPublication]
)

// ── F19 ───────────────────────────────────────────────────────────────────────

case class F19ContractSubDefence(
  meta:                 FormMeta,
  contractingAuthority: ContractingProfile,
  typeAndActivities:    Option[TypeAndActivities],
  contractObject:       F19ContractObject,
  lefti:                F19Lefti,
  procedure:            F19Procedure,
  complementaryInfo:    ComplementaryInfoNotice
)

case class F19ContractObject(
  title:        Option[RichText],
  description:  RichText,
  cpv:          Option[CpvCodes],
  location:     Option[LocationNuts]
)

case class F19Lefti(subContractConditions: Option[RichText])

case class F19Procedure(
  procedureType:   Option[ProcedureType],
  awardCriteria:   Option[AwardCriteriaDetail],
  receiptDeadline: Option[TedDateTime]
)

// ── T01 ───────────────────────────────────────────────────────────────────────

case class T01PriorInformationMove(
  meta:              FormMeta,
  authority:         ContractingProfile,
  typeAndActivities: Option[TypeAndActivities],
  contractObject:    T01ContractObject,
  lefti:             Option[F01Lefti],
  procedure:         T01Procedure,
  awards:            List[AwardOfContract],
  complementaryInfo: T01ComplementaryInfo
)

case class T01ContractObject(
  title:          Option[RichText],
  contractType:   Option[ContractType],
  description:    Option[RichText],
  cpv:            Option[CpvCodes],
  location:       Option[LocationNuts],
  estimatedValue: Option[FormContractValue]
)

case class T01Procedure(
  procedureType:   Option[RichText],
  fileReference:   Option[FileReference],
  receiptDeadline: Option[TedDateTime]
)

case class T01ComplementaryInfo(
  additionalInfo: Option[RichText],
  dispatchDate:   Option[StructuredDate]
)

// ── T02 ───────────────────────────────────────────────────────────────────────

case class T02ContractMove(
  meta:              FormMeta,
  authority:         ContractingProfile,
  typeAndActivities: Option[TypeAndActivities],
  contractObject:    T02ContractObject,
  lefti:             Option[F01Lefti],
  procedure:         T02Procedure,
  awards:            List[AwardOfContract],
  complementaryInfo: T01ComplementaryInfo
)

case class T02ContractObject(
  title:       Option[RichText],
  description: Option[RichText],
  cpv:         Option[CpvCodes],
  location:    Option[LocationNuts]
)

case class T02Procedure(
  procedureType:   Option[RichText],
  fileReference:   Option[FileReference],
  receiptDeadline: Option[TedDateTime],
  previousPub:     Option[PreviousPublication]
)

// ── OTH_NOT ───────────────────────────────────────────────────────────────────

case class OthNot(
  language: Language,
  category: FormCategory,
  content:  RichText
)

// ── EEIG ──────────────────────────────────────────────────────────────────────

case class Eeig(
  language:        Language,
  category:        FormCategory,
  groupingMembers: List[ContactData],
  content:         Option[RichText]
)