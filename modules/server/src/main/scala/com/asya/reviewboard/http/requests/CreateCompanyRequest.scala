package com.asya.reviewboard.http.requests

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec
import com.asya.reviewboard.domain.data.Company

final case class CreateCompanyRequest(
  name: String,
  url: String,
  location: Option[String] = None,
  country: Option[String] = None,
  industry: Option[String] = None,
  image: Option[String] = None,
  tags: Option[List[String]] = None
) {
  def toCompany(id: Long) =
    Company(
      id = id,
      slug = Company.makeSlug(name),
      name = name,
      url = url,
      location = location,
      country = country,
      industry = industry,
      image = image,
      tags = tags.getOrElse(List())
    )
}

object CreateCompanyRequest {
  given codec: JsonCodec[CreateCompanyRequest] = DeriveJsonCodec.gen[CreateCompanyRequest]
}
