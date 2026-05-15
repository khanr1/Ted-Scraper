package io.github.khanr1.tedscraper.common

import types.*

case class Login(
    esenderLogin: RichText,
    customerLogin: Option[RichText],
    loginClass: Option[LoginClass]
)
