package com.asya.reviewboard.http.endpoints

import sttp.tapir.*

trait HealthEndpoint {
  val healthEndpoint = endpoint
    .tag("health")
    .name("health")
    .description("Health check.")
    .get
    .in("health")
    .out(plainBody[String])
}
