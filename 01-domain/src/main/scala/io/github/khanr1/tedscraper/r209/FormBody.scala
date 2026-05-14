package io.github.khanr1.tedscraper.r209

import io.github.khanr1.tedscraper.r208.FormMeta

// ── sealed trait ──────────────────────────────────────────────────────────────

sealed trait FormBody

// ── F01_2014 — Prior Information Notice ──────────────────────────────────────
// XSD: 6 sections: LEGAL_BASIS, NOTICE, CONTRACTING_BODY, OBJECT_CONTRACT(×100),
//      LEFTI (opt), PROCEDURE, COMPLEMENTARY_INFO
case class F01PriorInfo2014(
  meta: FormMeta,
  legalBasis: Option[String],              // LEGAL_BASIS @VALUE
  noticeType: Option[String],              // NOTICE @TYPE (PRI_ONLY, PRI_CALL_COMPETITION, ...)
  contractingBody: ContractingBody,
  objectContracts: List[ObjectContract],   // maxOccurs=100
  lefti: Option[Lefti],
  procedure: Procedure,
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── F02_2014 — Contract Notice ────────────────────────────────────────────────
// XSD: LEGAL_BASIS, CONTRACTING_BODY, OBJECT_CONTRACT, LEFTI (opt), PROCEDURE,
//      COMPLEMENTARY_INFO
case class F02ContractNotice2014(
  meta: FormMeta,
  legalBasis: Option[String],
  contractingBody: ContractingBody,
  objectContract: ObjectContract,
  lefti: Option[Lefti],
  procedure: Procedure,
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── F03_2014 — Contract Award Notice ─────────────────────────────────────────
// XSD: LEGAL_BASIS (opt in S01 files), CONTRACTING_BODY, OBJECT_CONTRACT,
//      PROCEDURE, AWARD_CONTRACT(×10000), COMPLEMENTARY_INFO. NO LEFTI.
case class F03ContractAward2014(
  meta: FormMeta,
  legalBasis: Option[String],
  contractingBody: ContractingBody,
  objectContract: ObjectContract,
  procedure: Procedure,
  awards: List[AwardContractItem],         // AWARD_CONTRACT elements
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── F04_2014 — Periodic Indicative Notice (Utilities) ────────────────────────
case class F04PeriodicIndicative2014(
  meta: FormMeta,
  legalBasis: Option[String],
  contractingBody: ContractingBody,
  objectContracts: List[ObjectContract],
  lefti: Option[Lefti],
  procedure: Procedure,
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── F05_2014 — Contract Notice (Utilities) ────────────────────────────────────
case class F05ContractNoticeUtilities2014(
  meta: FormMeta,
  legalBasis: Option[String],
  contractingBody: ContractingBody,
  objectContract: ObjectContract,
  lefti: Option[Lefti],
  procedure: Procedure,
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── F06_2014 — Contract Award Notice (Utilities) ─────────────────────────────
case class F06ContractAwardUtilities2014(
  meta: FormMeta,
  legalBasis: Option[String],
  contractingBody: ContractingBody,
  objectContract: ObjectContract,
  procedure: Procedure,
  awards: List[AwardContractItem],
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── F08_2014 — Buyer Profile ──────────────────────────────────────────────────
// XSD: LEGAL_BASIS, CONTRACTING_BODY, OBJECT_CONTRACT, COMPLEMENTARY_INFO.
//      NO LEFTI, NO PROCEDURE, NO AWARD_CONTRACT.
case class F08BuyerProfile2014(
  meta: FormMeta,
  legalBasis: Option[String],
  contractingBody: ContractingBody,
  objectContract: ObjectContract,
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── F12_2014 — Design Contest Notice ─────────────────────────────────────────
case class F12DesignContest2014(
  meta: FormMeta,
  legalBasis: Option[String],
  contractingBody: ContractingBody,
  objectContract: ObjectContract,
  lefti: Option[Lefti],
  procedure: Procedure,
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── F13_2014 — Results of Design Contest ─────────────────────────────────────
case class F13DesignContestResult2014(
  meta: FormMeta,
  legalBasis: Option[String],
  contractingBody: ContractingBody,
  objectContract: ObjectContract,
  procedure: Procedure,
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── F14_2014 — Corrigendum ────────────────────────────────────────────────────
// XSD: LEGAL_BASIS, CONTRACTING_BODY, OBJECT_CONTRACT, COMPLEMENTARY_INFO,
//      CHANGES. NO LEFTI, NO PROCEDURE, NO AWARD_CONTRACT.
case class F14Corrigendum2014(
  meta: FormMeta,
  legalBasis: Option[String],
  contractingBody: ContractingBody,
  objectContract: ObjectContract,
  complementaryInfo: ComplementaryInfo,
  changes: List[Change]                    // CHANGES > CHANGE elements
) extends FormBody

// ── F15_2014 — Voluntary Ex Ante Transparency Notice (VEAT) ──────────────────
// XSD: LEGAL_BASIS (choice of 4 directives), CONTRACTING_BODY, OBJECT_CONTRACT,
//      PROCEDURE, AWARD_CONTRACT(×10000), COMPLEMENTARY_INFO. NO LEFTI.
case class F15Veat2014(
  meta: FormMeta,
  legalBasis: Option[String],              // LEGAL_BASIS @VALUE or DIRECTIVE @VALUE
  contractingBody: ContractingBody,
  objectContract: ObjectContract,
  procedure: Procedure,
  awards: List[AwardContractItem],
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── F20_2014 — Modification Notice ───────────────────────────────────────────
case class F20Modification2014(
  meta: FormMeta,
  legalBasis: Option[String],
  contractingBody: ContractingBody,
  objectContract: ObjectContract,
  procedure: Option[Procedure],
  award: Option[AwardContractItem],
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── F21_2014 — Social and Other Specific Services ────────────────────────────
case class F21SocialServices2014(
  meta: FormMeta,
  legalBasis: Option[String],
  contractingBody: ContractingBody,
  objectContracts: List[ObjectContract],
  lefti: Option[Lefti],
  procedure: Option[Procedure],
  awards: List[AwardContractItem],
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── F24_2014 — Concession Notice ─────────────────────────────────────────────
case class F24ConcessionNotice2014(
  meta: FormMeta,
  legalBasis: Option[String],
  contractingBody: ContractingBody,
  objectContract: ObjectContract,
  lefti: Option[Lefti],
  procedure: Procedure,
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── F25_2014 — Concession Award Notice ───────────────────────────────────────
case class F25ConcessionAward2014(
  meta: FormMeta,
  legalBasis: Option[String],
  contractingBody: ContractingBody,
  objectContract: ObjectContract,
  procedure: Procedure,
  awards: List[AwardContractItem],
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── MOVE — Railway Services Contract ─────────────────────────────────────────
case class Move2014(
  meta: FormMeta,
  contractingBody: ContractingBody,
  objectContract: ObjectContract,
  complementaryInfo: ComplementaryInfo
) extends FormBody

// ── Fallback ──────────────────────────────────────────────────────────────────
case class UnknownForm2014(meta: FormMeta, formType: String) extends FormBody
