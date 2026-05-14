package io.github.khanr1.tedscraper.r208.types

enum SuppliesType:
    case HirePurchase, Rental, Lease, Purchase, CombinationThese

object SuppliesType:
    private val mapping = Map(
        "hire-purchase" -> HirePurchase,
        "rental" -> Rental,
        "lease" -> Lease,
        "purchase" -> Purchase,
        "combination-these" -> CombinationThese
    ) 
    private val reverseMapping = mapping.map(_.swap)
    def fromString(s: String): SuppliesType = mapping.getOrElse(s, Purchase)
    def from(s: String): Option[SuppliesType] = mapping.get(s.toLowerCase)
