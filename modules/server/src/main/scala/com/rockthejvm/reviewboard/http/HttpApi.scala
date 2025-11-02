package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.http.controllers.*
import sttp.tapir.server.ServerEndpoint
import zio.{Task, UIO}

object HttpApi {
  private def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  private def makeControllers = for {
    healthController  <- HealthController.makeZIO
    companyController <- CompanyController.makeZIO
  } yield List(healthController, companyController)

  val endpointsZIO: UIO[List[ServerEndpoint[Any, Task]]] = makeControllers.map(gatherRoutes)
}
