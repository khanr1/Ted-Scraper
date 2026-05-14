package io.github.khanr1.tedscraper.r208

import types.*

sealed trait PurchasingOnBehalf
case class PurchasingYes(onBehalfOf: List[ContactData]) extends PurchasingOnBehalf
case object PurchasingNo                                extends PurchasingOnBehalf
