package com.asya.reviewboard.http.controllers

import com.asya.reviewboard.http.endpoints.HealthEndpoint
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import zio.*

class HealthController private extends BaseController with HealthEndpoint {
  val health: ServerEndpoint[Any, Task] = healthEndpoint
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

  val error: ServerEndpoint[Any, Task] = errorEndpoint
    .serverLogic[Task](_ => ZIO.fail(new RuntimeException("Boom!")).either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(health, error)
}

object HealthController {
  val makeZIO: UIO[HealthController] = ZIO.succeed(new HealthController)
}
