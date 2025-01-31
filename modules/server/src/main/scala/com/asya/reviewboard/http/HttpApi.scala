package com.asya.reviewboard.http

import com.asya.reviewboard.http.controllers.*
import com.asya.reviewboard.services.{CompanyService, ReviewService}
import sttp.tapir.server.ServerEndpoint
import zio.{Task, URIO}

object HttpApi {
  private def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  private def makeControllers = for {
    healthController  <- HealthController.makeZIO
    companyController <- CompanyController.makeZIO
    reviewController  <- ReviewController.makeZIO
  } yield List(healthController, companyController, reviewController)

  val endpointsZIO: URIO[CompanyService & ReviewService, List[ServerEndpoint[Any, Task]]] =
    makeControllers.map(gatherRoutes)
}
