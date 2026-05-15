package io.github.khanr1.tedscraper.common
package types

enum ContractType:
    case Works, Services, Supplies

object ContractType:
    private val mapping = Map(
        "works" -> Works,
        "services" -> Services,
        "supplies" -> Supplies
    )
    private val reverseMapping = mapping.map(_.swap)
    def fromString(s: String): ContractType = mapping.getOrElse(s, Services)
    def from(s: String): Option[ContractType] = mapping.get(s.toLowerCase)