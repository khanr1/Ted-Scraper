package io.github.khanr1.tedscraper.r208
package types

enum LoginClass: 
    case A
    case B
    case C
    case D
    case Unknown

object LoginClass:
    def mapping: Map[String, LoginClass] = Map(
        "A" -> A,
        "B" -> B,
        "C" -> C,
        "D" -> D
    )
    def reverseMapping: Map[LoginClass, String] = mapping.map(_.swap)
    def from(s: String): LoginClass = mapping.getOrElse(s, Unknown)
