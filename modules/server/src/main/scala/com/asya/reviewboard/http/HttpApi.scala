package com.asya.reviewboard.http

import com.asya.reviewboard.http.controllers.*

object HttpApi {
  private def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  private def makeControllers = for {
    healthController <- HealthController.makeZIO
    companyController <- CompanyController.makeZIO
  } yield List(healthController, companyController)

  val endpointsZIO = makeControllers.map(gatherRoutes)
}
