package io.github.khanr1.tedscraper.r208

import types.*

case class Sender(
    login: Login,
    user: Option[SenderUser],
    noDocExt: ExternalDocRef
)
