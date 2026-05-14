package io.github.khanr1.tedscraper.api

import com.raquo.airstream.web.FetchStream

object MessageAPI {
  private val endpoint: String = "/api/hello"

  def getMessage =
    FetchStream
      .get(endpoint)

}
