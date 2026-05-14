package io.github.khanr1.tedscraper.r209

import io.github.khanr1.tedscraper.r208.types.Url

// Mirrors CONTRACTING_BODY (body_fXX types in XSD) — Section I of all forms.
case class ContractingBody(
  address: AddressContractingBody,
  additionalAddresses: List[AddressContractingBody], // ADDRESS_CONTRACTING_BODY_ADDITIONAL
  jointProcurement: Boolean,                          // JOINT_PROCUREMENT_INVOLVED
  centralPurchasing: Boolean,                         // CENTRAL_PURCHASING
  authorityType: Option[String],                      // CA_TYPE @VALUE or CA_TYPE_OTHER text
  authorityActivity: Option[String],                  // CA_ACTIVITY @VALUE or CA_ACTIVITY_OTHER text
  entityActivity: Option[String],                     // CE_ACTIVITY @VALUE or CE_ACTIVITY_OTHER (utilities)
  documentAccess: Option[Boolean],                    // Some(true)=DOCUMENT_FULL, Some(false)=DOCUMENT_RESTRICTED
  urlDocument: Option[Url]                            // URL_DOCUMENT
)
