package io.github.khanr1.tedscraper.r208

import types.*

case class LinkSection(
    xmlSchemaDefinitionLink: Option[Link],
    officialFormsLink: Option[Link],
    formsLabelsLink: Option[Link],
    originalCpvLink: Option[Link],
    originalNutsLink: Option[Link]
)

/** xlink-typed hyperlink (all_links in common_prod.xsd). href and linkType are
  * required attributes.
  */
case class Link(href: Url, linkType: String, title: Option[RichText])
