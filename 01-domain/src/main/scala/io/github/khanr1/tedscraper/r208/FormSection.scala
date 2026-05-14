package io.github.khanr1.tedscraper.r208

case class FormSection(forms: List[FormBody])


sealed trait FormBody

case class PriorInformationForm(data: F01PriorInformation)              extends FormBody
case class ContractNoticeForm(data: F02ContractNotice)                   extends FormBody
case class ContractAwardForm(data: F03ContractAward)                     extends FormBody
case class PeriodicIndicativeUtilitiesForm(data: F04PeriodicIndicativeUtilities) extends FormBody
case class ContractUtilitiesForm(data: F05ContractUtilities)             extends FormBody
case class ContractAwardUtilitiesForm(data: F06ContractAwardUtilities)   extends FormBody
case class QualificationSystemUtilitiesForm(data: F07QualificationSystemUtilities) extends FormBody
case class BuyerProfileForm(data: F08BuyerProfile)                       extends FormBody
case class SimplifiedContractForm(data: F09SimplifiedContract)           extends FormBody
case class ConcessionForm(data: F10Concession)                           extends FormBody
case class ContractConcessionaireForm(data: F11ContractConcessionaire)   extends FormBody
case class DesignContestForm(data: F12DesignContest)                     extends FormBody
case class ResultDesignContestForm(data: F13ResultDesignContest)         extends FormBody
case class AdditionalInformationCorrigendumForm(data: F14AdditionalInformationCorrigendum) extends FormBody
case class VeatForm(data: F15VeatNotice)                                  extends FormBody
case class PriorInformationDefenceForm(data: F16PriorInformationDefence) extends FormBody
case class ContractDefenceForm(data: F17ContractDefence)                 extends FormBody
case class ContractAwardDefenceForm(data: F18ContractAwardDefence)       extends FormBody
case class ContractSubDefenceForm(data: F19ContractSubDefence)           extends FormBody
case class PriorInformationMoveForm(data: T01PriorInformationMove)       extends FormBody
case class ContractMoveForm(data: T02ContractMove)                        extends FormBody
case class OtherNoticeForm(data: OthNot)                                  extends FormBody
case class EeigForm(data: Eeig)                                           extends FormBody