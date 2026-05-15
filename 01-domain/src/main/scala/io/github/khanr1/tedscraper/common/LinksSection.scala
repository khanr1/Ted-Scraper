package io.github.khanr1.tedscraper.common

case class LinksSection(
    xmlSchemaDefinitionLink: Option[Link],
    officialFormsLink: Option[Link],
    formsLabelsLink: Option[Link],
    originalCpvLink: Option[Link],
    originalNutsLink: Option[Link]
)
