package io.github.khanr1.tedscraper.r208

import io.github.khanr1.tedscraper.common.types.*
import io.github.khanr1.tedscraper.common.*

/** TED Notice, as defined in the TED R2.08 schema.
  * @param docId
  *   Validated pattern [0-9]{1,6}-20[0-9]{2}
  * @param edition
  *   Validated pattern 20[0-9]{2}[0-9]{1,3}
  * @param schemaVersion
  *   Inferred from namespace (not an XSD attribute)
  * @param sender
  *   Optional — only present in eSender submissions /
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
