package com.asya.reviewboard.http.endpoints

import com.asya.reviewboard.domain.data.*
import com.asya.reviewboard.http.requests.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait ReviewEndpoints extends BaseEndpoint {
  val createEndpoint = baseEndpoint
    .tag("reviews")
    .name("create")
    .description("Create a Review.")
    .in("reviews")
    .post
    .in(jsonBody[CreateReviewRequest])
    .out(jsonBody[Review])

  val getByIdEndpoint = baseEndpoint
    .tag("reviews")
    .name("getById")
    .description("Get Review by id.")
    .in("reviews" / path[Long]("id"))
    .get
    .out(jsonBody[Option[Review]])

  val getByCompanyIdEndpoint = baseEndpoint
    .tag("reviews")
    .name("getByCompanyId")
    .description("Get Reviews by company.")
    .in("reviews" / "company" / path[Long]("id"))
    .get
    .out(jsonBody[List[Review]])
}
