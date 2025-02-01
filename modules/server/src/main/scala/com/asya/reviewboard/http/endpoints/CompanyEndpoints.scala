package com.asya.reviewboard.http.endpoints

import com.asya.reviewboard.domain.data.*
import com.asya.reviewboard.http.requests.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait CompanyEndpoints extends BaseEndpoint {
  val createEndpoint = baseEndpoint
    .tag("companies")
    .name("create")
    .description("Create a Company.")
    .in("companies")
    .post
    .in(jsonBody[CreateCompanyRequest])
    .out(jsonBody[Company])

  val getAllEndpoint = baseEndpoint
    .tag("companies")
    .name("getAll")
    .description("Get all Companies.")
    .in("companies")
    .get
    .out(jsonBody[List[Company]])

  val getByIdEndpoint = baseEndpoint
    .tag("companies")
    .name("getById")
    .description("Get Company by id.")
    .in("companies" / path[String]("id"))
    .get
    .out(jsonBody[Option[Company]])
}
