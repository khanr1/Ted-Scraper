package io.github.khanr1.tedscraper.r209

import io.github.khanr1.tedscraper.common.RichText

// Procedure type as defined by element presence in PROCEDURE section.
// Maps: PT_OPEN, PT_RESTRICTED, PT_COMPETITIVE_NEGOTIATION, PT_COMPETITIVE_DIALOGUE,
//       PT_INNOVATION_PARTNERSHIP, PT_NEGOTIATED_WITH_PRIOR_CALL,
//       PT_AWARD_CONTRACT_WITHOUT_CALL, PT_AWARD_CONTRACT_WITHOUT_PUBLICATION
enum ProcedureType2014:
  case Open
  case Restricted
  case CompetitiveNegotiation
  case CompetitiveDialogue
  case InnovationPartnership
  case NegotiatedWithPriorCall
  case AwardWithoutCall
  case AwardWithoutPublication
  case Unknown(raw: String)

// Mirrors PROCEDURE element (procedure_fXX types in XSD) — Section IV.
case class Procedure(
  procedureType: Option[ProcedureType2014], // detected by child element presence
  accelerated: Option[RichText],            // ACCELERATED_PROC
  framework: Boolean,                       // FRAMEWORK presence
  dps: Boolean,                             // DPS presence
  eauctionUsed: Boolean,                    // EAUCTION_USED
  gpa: Option[Boolean],                     // CONTRACT_COVERED_GPA / NO_CONTRACT_COVERED_GPA
  noticeNumberOj: Option[String],           // NOTICE_NUMBER_OJ (prior publication reference)
  justification: Option[RichText],          // D_JUSTIFICATION from Annex D
  dateReceiptTenders: Option[String],       // DATE_RECEIPT_TENDERS ISO
  timeReceiptTenders: Option[String],       // TIME_RECEIPT_TENDERS
  languages: List[String],                  // LANGUAGE @VALUE within LANGUAGES
  dateAwardScheduled: Option[String]        // DATE_AWARD_SCHEDULED ISO
)
