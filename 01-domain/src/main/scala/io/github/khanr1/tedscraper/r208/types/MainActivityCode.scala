package io.github.khanr1.tedscraper.r208.types

enum MainActivityCode:
    case A  // General public services
    case B  // Defence
    case C  // Public order and safety
    case D  // Environment
    case E  // Economic and financial affairs
    case F  // Health
    case G  // Housing and community amenities
    case H  // Social protection
    case I  // Recreation, culture and religion
    case J  // Education
    case K  // Other
    case L  // Education (alternate)
    case M  // Construction and engineering
    case N  // Not specified
    case P  // Postal services
    case R  // Railway services
    case S  // Water
    case T  // Urban railway / tramway / trolleybus / bus
    case U  // Port-related
    case W  // Exploration/extraction gas/oil
    case Z  // Not applicable
    case `8` // Not specified (numeric)
    case `9` // Other (numeric)
    case Unknown(raw: String)

object MainActivityCode:
    private val mapping: Map[String, MainActivityCode] = Map("General public services" -> A, "Defence" -> B, "Public order and safety" -> C, "Environment" -> D, "Economic and financial affairs" -> E, "Health" -> F, "Housing and community amenities" -> G, "Social protection" -> H, "Recreation, culture and religion" -> I, "Education" -> J, "Other" -> K, "Education (alternate)" -> L, "Construction and engineering" -> M, "Not specified" -> N, "Postal services" -> P, "Railway services" -> R, "Water" -> S, "Urban railway / tramway / trolleybus / bus" -> T, "Port-related" -> U, "Exploration/extraction gas/oil" -> W, "Not applicable" -> Z, "Not specified (numeric)" -> `8`, "Other (numeric)" -> `9`)
    private val reverseMapping: Map[MainActivityCode, String] = mapping.map(_.swap)
    def from(s: String): MainActivityCode = mapping.getOrElse(s, Unknown(s))