package com.asya.reviewboard.http.endpoints

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
import com.asya.reviewboard.http.requests.*
import com.asya.reviewboard.domain.data.*

trait CompanyEndpoints {
  val createEndpoint = endpoint
    .tag("companies")
    .name("create")
    .description("Create a Company.")
    .in("companies")
    .post
    .in(jsonBody[CreateCompanyRequest])
    .out(jsonBody[Company])

  val getAllEndpoint = endpoint
    .tag("companies")
    .name("getAll")
    .description("Get all Companies.")
    .in("companies")
    .get
    .out(jsonBody[List[Company]])

  val getByIdEndpoint = endpoint
    .tag("companies")
    .name("getById")
    .description("Get Company by id.")
    .in("companies" / path[String]("id"))
    .get
    .out(jsonBody[Option[Company]])  
}
