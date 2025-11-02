package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody

trait CompanyEndpoints {
  val createEndpoint = endpoint
    .tag("companies")
    .name("create")
    .description("Create a company.")
    .in("companies")
    .post
    .in(jsonBody[CreateCompanyRequest])
    .out(jsonBody[Company])

  val getAllEndpoint = endpoint
    .tag("companies")
    .name("getAll")
    .description("Get all companies.")
    .in("companies")
    .get
    .out(jsonBody[List[Company]])

  val getByIdEndpoint = endpoint
    .tag("companies")
    .name("getById")
    .description("Get company by id.")
    .in("companies" / path[String]("id"))
    .get
    .out(jsonBody[Option[Company]])
}
