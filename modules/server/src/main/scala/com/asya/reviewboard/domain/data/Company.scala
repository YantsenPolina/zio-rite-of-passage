package com.asya.reviewboard.domain.data

import zio.json.{DeriveJsonCodec, JsonCodec}

final case class Company(
    id: Long,
    slug: String,
    name: String,
    url: String,
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: List[String] = List()
)

object Company {
  given codec: JsonCodec[Company] = DeriveJsonCodec.gen[Company]

  def makeSlug(name: String): String =
    name
      .replace(" +", " ")
      .split(" ")
      .map(_.toLowerCase())
      .mkString("-")
}
