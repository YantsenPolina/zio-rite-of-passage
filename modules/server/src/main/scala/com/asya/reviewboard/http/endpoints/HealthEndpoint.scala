package com.asya.reviewboard.http.endpoints

import sttp.tapir.*

trait HealthEndpoint extends BaseEndpoint {
  val healthEndpoint = baseEndpoint
    .tag("health")
    .name("health")
    .description("Health check.")
    .get
    .in("health")
    .out(plainBody[String])

  val errorEndpoint = baseEndpoint
    .tag("health")
    .name("error")
    .description("Error.")
    .get
    .in("health" / "error")
    .out(plainBody[String])
}
