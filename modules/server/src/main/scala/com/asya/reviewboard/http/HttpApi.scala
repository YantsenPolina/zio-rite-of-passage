package com.asya.reviewboard.http

import com.asya.reviewboard.http.controllers.*
import com.asya.reviewboard.services.CompanyService
import sttp.tapir.server.ServerEndpoint
import zio.{Task, URIO}

object HttpApi {
  private def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  private def makeControllers = for {
    healthController  <- HealthController.makeZIO
    companyController <- CompanyController.makeZIO
  } yield List(healthController, companyController)

  val endpointsZIO: URIO[CompanyService, List[ServerEndpoint[Any, Task]]] =
    makeControllers.map(gatherRoutes)
}
