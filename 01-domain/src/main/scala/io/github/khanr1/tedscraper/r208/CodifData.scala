package io.github.khanr1.tedscraper.r208

import types.*

/** CODIF_DATA (t_codif_data — common_prod.xsd). Every CODE attribute is now a
  * typed enum, not a String.
  */
case class CodifData(
    dispatchDate: TedDate,
    documentRequestDate: Option[TedDateTime],
    submissionDate: Option[TedDateTime],
    authorityType: AuthorityTypeField,
    documentType: DocumentTypeField,
    contractNature: ContractNatureField,
    procedure: ProcedureField,
    regulation: RegulationField,
    typeBid: TypeBidField,
    awardCriteria: AwardCritField,
    mainActivities: List[MainActivityField],
    heading: HeadingCode,
    directive: Option[Directive]
)

case class AuthorityTypeField(code: AuthorityTypeCode, label: RichText)
case class DocumentTypeField(code: DocumentTypeCode, label: RichText)
case class ContractNatureField(code: ContractNatureCode, label: RichText)
case class ProcedureField(code: ProcedureCode, label: RichText)
case class RegulationField(code: RegulationCode, label: RichText)
case class TypeBidField(code: TypeBidCode, label: RichText)
case class AwardCritField(code: AwardCritCode, label: RichText)
case class MainActivityField(code: MainActivityCode, label: RichText)
