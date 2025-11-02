package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*

trait HealthEndpoint {
  val healthEndpoint = endpoint
    .tag("health")
    .name("health")
    .description("Health check.")
    .in("health")
    .get
    .out(plainBody[String])
}
