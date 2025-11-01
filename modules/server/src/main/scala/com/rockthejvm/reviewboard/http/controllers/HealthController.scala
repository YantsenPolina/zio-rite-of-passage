package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.http.endpoints.HealthEndpoint
import zio.*

class HealthController private extends HealthEndpoint {
  val health = healthEndpoint
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))
}

object HealthController {
  val makeZIO: UIO[HealthController] = ZIO.succeed(new HealthController)
}
