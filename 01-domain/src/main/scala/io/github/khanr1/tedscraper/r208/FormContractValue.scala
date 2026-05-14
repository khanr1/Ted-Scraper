package io.github.khanr1.tedscraper.r208

import types.*

sealed trait FormContractValue
case class ExactValue(value: ValueCost)    extends FormContractValue
case class RangeValue(range: RangeValueCost) extends FormContractValue

case class ValueCost(amount: MonetaryAmount, currency: Currency)
case class RangeValueCost(low: MonetaryAmount, high: MonetaryAmount, currency: Currency)

