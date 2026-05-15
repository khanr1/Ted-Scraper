package io.github.khanr1.tedscraper.common

import types.*

case class Sender(
    login: Login,
    user: Option[SenderUser],
    noDocExt: ExternalDocRef
)
