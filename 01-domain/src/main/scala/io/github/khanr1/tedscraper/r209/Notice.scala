package io.github.khanr1.tedscraper.r209

import io.github.khanr1.tedscraper.r208.{
  TechnicalSection, LinksSection, Sender,
  CodedDataSection, TranslationSection
}
import io.github.khanr1.tedscraper.r208.types.{DocumentId, Edition}
import types.SchemaVersion

/** TED Notice parsed from the R2.0.9 (2014-directive) schema family.
 *
 *  The envelope sections (TechnicalSection, CodedDataSection, etc.) have
 *  identical XML structure in R2.0.8 and R2.0.9, so they are reused from
 *  the r208 domain. Only FormSection and SchemaVersion differ.
 */
final case class Notice(
  docId: DocumentId,
  edition: Edition,
  schemaVersion: SchemaVersion,
  technicalSection: TechnicalSection,
  linkSection: LinksSection,
  sender: Option[Sender],
  codedDataSection: CodedDataSection,
  translationSection: TranslationSection,
  formSection: FormSection
)
