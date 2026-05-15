# eformNotice.csv vs data2.csv — Comparison Report

**Scope:** 61 notices appear in both files (74 unique IDs in eformNotice.csv, 276 in data2.csv).
13 notices are present only in eformNotice.csv (all from 2025–2026, i.e. newer than data2.csv coverage).
215 notices are present only in data2.csv (older notices / different procurement types not in the eforms test set).

---

## Column mapping used

| eformNotice.csv column | data2.csv column |
|---|---|
| `notice_oj_id` | `Notice ID` |
| `publication_date` | `Publication date` |
| `ca_name` | `Contracting authority name` |
| `ca_country` | `Contracting authority country code` |
| `award_contract_number` | `ContractID` |
| `award_contract_title` | `Title` |
| `lot_description` | `Description` |
| `total_final_value` | `Value` |
| `total_final_currency` | `Currency` |
| `winner_name` | `Awarded supplier` |
| `winner_country` | `Awarded supplier country code` |
| `procedure_justification` | `Justification` |

---

## Field-by-field findings

### ✅ `notice_oj_id` — Perfect match
All 61 common notices agree. The `YYYY/S NNN-NNNNNN` format constructed from
`efac:Publication/efbc:GazetteID` + `efbc:NoticePublicationID` matches data2 exactly.

---

### ⚠️ `publication_date` — Always differs (all 61 notices)

| data2 | eformNotice.csv |
|---|---|
| `2023-09-19` | `2023-09-15+02:00` |
| `2024-01-02` | `2023-12-28+01:00` |

**Root cause:** eformNotice.csv uses `cbc:IssueDate` (the date the notice was *sent* to TED),
which is 1–4 days earlier than the actual OJ publication date. data2 uses the publication date
from the OJ (when the notice appeared in print/online).

**Fix:** Parse `efac:Publication/cbc:PublicationDate` instead of `cbc:IssueDate` for this column.
The publication date is already present in the eforms extension alongside `GazetteID`.

---

### ⚠️ `ca_name` — Agree on content, differ on whitespace (all 61 agree after normalization)

**Root cause:** The eforms XML encodes multi-line organization names with XML indentation preserved
verbatim. The parser captures `\n` and leading spaces; data2 collapses whitespace.

**Fix:** Apply `text.replaceAll("\\s+", " ").trim` when extracting `cac:PartyName/cbc:Name`
from company nodes.

---

### ⚠️ `ca_country` — Always differs (all 61 notices)

| data2 | eformNotice.csv |
|---|---|
| `DE` | `DEU` |
| `BE` | `BEL` |
| `FI` | `FIN` |

**Root cause:** data2 uses ISO 3166-1 **alpha-2** (2-letter). eForms XML stores
`cbc:IdentificationCode listName="country"` in **alpha-3** (3-letter), which the
parser copies verbatim.

**Fix:** Add an ISO 3166-1 alpha-3 → alpha-2 lookup in the parser (or service layer),
analogous to the existing `CountryCode` enum in the `common` package.

---

### ℹ️ `award_contract_number` — Semantic difference (not a bug)

data2 stores the **Lot ID** (e.g. `LOT-0001`) as ContractID.
eformNotice.csv stores the **buyer-assigned contract reference** (BT-150,
`efac:ContractReference/cbc:ID`, e.g. `BLUEFORS1`, `VERTRAG-0001`).

These are different concepts. data2 uses the Lot ID as a proxy because eForms always assigns
a lot, while the actual contract reference number is optional (8 of 61 notices have no BT-150,
leaving `award_contract_number` empty in eformNotice.csv).

**Recommendation:** The eformNotice.csv behaviour is correct per the eForms data model.
Consider adding `lot_number` as an additional column for cross-referencing.

---

### ⚠️ `award_contract_title` — Differ in 14 of 61 notices

47/61 notices agree (after whitespace normalization). The 14 that differ fall into two patterns:

**Pattern A — eforms has supplier name + address instead of lot title (11 notices):**
| data2 (lot title) | eformNotice.csv (BT-721 contract title) |
|---|---|
| `Beschaffung eines Kryostats` | `IceOxford` |
| `Kompakter tabletop Entmischungskryostat…` | `Qinu GmbH, 76137 Karlsruhe` |
| `Closed-Cycle refrigerator Cryostat system` | `P1 Closed-Cycle refrigerator Cryostat system` |

**Root cause:** The service fills `award_contract_title` from `SettledContract.title` (BT-721).
Some contracting authorities populate BT-721 with the supplier name / internal reference rather
than the descriptive lot title. data2 uses the lot title (BT-21-Lot) for this column.

**Fix:** Prefer the lot title (`lot.scope.lotTitle`) over `contract.title` for this column, using
the contract title only as a fallback when the lot title is absent.

**Pattern B — minor text differences (3 notices):**
e.g. `2024_AOO_CRYOSTAT_LPENS` vs `2024 AOO CRYOSTAT LPENS` — different source systems
(eSender vs TED portal) normalise underscores/spaces differently. Not actionable.

---

### ✅ `lot_description` — Agree on content (all 61 agree after whitespace normalization)

Same whitespace issue as `ca_name`: XML indentation preserved in the eforms parser.

**Fix:** Same whitespace normalization as `ca_name`.

---

### ⚠️ `total_final_value` — Format differs (47/61); 4 genuinely different amounts

**Format issue (47 notices, fixable):**
data2 stores amounts as integers (`321000`); eformNotice.csv stores as floats (`321000.0`).
Both represent the same value.

**Fix:** Format `tender.payableAmount.value` as a long integer when it has no fractional part
(e.g. `BigDecimal(v).toInt.toString` or `f"%.0f"`).

**Genuine discrepancies (4 notices):**

| Notice ID | data2 | eformNotice.csv | Likely cause |
|---|---|---|---|
| `2024/S 233-729960` | 291 336.67 | 874 010.00 | data2 = per-lot share; eforms = total awarded amount |
| `2025/S 123-422765` | 725 000 | 1 450 000 | data2 = half the eforms value (possible lot split) |
| `2025/S 157-541259` | 0 | 500 000 (×3 lots) | data2 = 0; eforms captures lot tender values |
| `2025/S 47-149514` | 1 629 780 | 4 889 340 | data2 ≈ 1/3 of eforms value (possible lot share) |

These suggest data2 sometimes stores the per-supplier share while eformNotice.csv stores the
full awarded tender value. No code change needed; this is a data2 modelling difference.

---

### ✅ `total_final_currency` — Mostly match (53/61 agree)

1 notice has currency in eformNotice.csv but not in data2; 7 have no value or currency in either.

---

### ⚠️ `winner_name` — 4 discrepancies out of 61

| Notice ID | data2 | eformNotice.csv | Cause |
|---|---|---|---|
| `2024/S 32-94768` | `no awarded supplier` | `Bluefors` | data2 marks as no-award; XML has a winner |
| `2025/S 157-541259` | `Zero Point Cryogenics` | `Maybell Quantum` | Different lot winner |
| `2025/S 2-2838` | `BLUEFORS` | `BLUEFORS OY` | data2 truncated legal name |
| `2025/S 35-112986` | `Oxford Instruments GmbH` | `OXFORD INSTRUMENT` | data2 has full name; eforms truncated |

No systemic fix needed. The last two are data quality differences between eSender submissions
and data2's source.

---

### ⚠️ `winner_country` — Always differs (same alpha-2 vs alpha-3 issue)

Identical root cause and fix as `ca_country`. eformNotice.csv stores alpha-3 (`FIN`, `GBR`);
data2 stores alpha-2 (`FI`, `UK`).

Note: data2 uses `UK` (non-standard) for the United Kingdom while ISO alpha-2 is `GB`.

---

### ⚠️ `procedure_justification` — Agree on content; differ on whitespace (18 notices)

41/61 notices have no justification in either file (correct — only direct-award notices carry one).
Of the 20 with content, 18 agree after whitespace normalization; 2 have data in data2 but not in
eformNotice.csv (the parser missed their `ProcessJustification` block).

**Fix (whitespace):** Same whitespace normalization as `ca_name`.

**Fix (2 missing):** Investigate whether those 2 notices use a non-standard `listName` on
`cbc:ProcessReasonCode` that the current filter (`listName="direct-award-justification"`) excludes.

---

## Summary of actionable fixes

| Priority | Issue | Fix |
|---|---|---|
| **High** | `ca_country` / `winner_country` always wrong | Add ISO alpha-3 → alpha-2 conversion |
| **High** | `publication_date` off by 1–4 days | Use `efac:Publication/cbc:PublicationDate` |
| **Medium** | Whitespace in `ca_name`, `lot_description`, `procedure_justification` | `text.replaceAll("\\s+", " ").trim` on all text fields |
| **Medium** | `award_contract_title` uses supplier name instead of lot title | Prefer `lot.scope.lotTitle` over `contract.title` |
| **Low** | `total_final_value` format — int vs float | Format as integer when fractional part is zero |
| **Info** | `award_contract_number` semantics differ | Documented; not a bug |
| **Info** | 4 genuinely different amounts | Data2 modelling difference; not actionable |

---

## Coverage

| | Count |
|---|---|
| Notices in eformNotice.csv | 74 |
| Notices in data2.csv | 276 |
| Common (comparable) | 61 |
| Only in eformNotice.csv (2025–2026, newer) | 13 |
| Only in data2.csv (older / different types) | 215 |
| eformNotice.csv columns not present in data2 | `sdk_version`, `dispatch_date`, `authority_type`, `contract_type`, `cpv_code`, `cpv_additional`, `nuts_code`, `procedure_type`, `award_criteria_summary`, `lot_cpv`, `award_date`, `estimated_value`, `estimated_currency`, `offers_received`, `winner_town`, `ref_notice_number`, `relates_to_eu_project` |
