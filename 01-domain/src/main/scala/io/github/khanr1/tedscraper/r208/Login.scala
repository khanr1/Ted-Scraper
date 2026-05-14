package io.github.khanr1.tedscraper.r208

import types.*

case class Login(
    esenderLogin: RichText,
    customerLogin: Option[RichText],
    loginClass: Option[LoginClass]
)
