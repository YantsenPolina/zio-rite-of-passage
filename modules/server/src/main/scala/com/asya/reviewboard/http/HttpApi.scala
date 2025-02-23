package com.asya.reviewboard.http

import com.asya.reviewboard.http.controllers.*
import com.asya.reviewboard.services.{CompanyService, JwtService, ReviewService, UserService}
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

object HttpApi {
  private def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  private def makeControllers = for {
    healthController  <- HealthController.makeZIO
    companyController <- CompanyController.makeZIO
    reviewController  <- ReviewController.makeZIO
    userController    <- UserController.makeZIO
  } yield List(healthController, companyController, reviewController, userController)

  val endpointsZIO: ZIO[JwtService & UserService & ReviewService & CompanyService, Nothing, List[
    ServerEndpoint[Any, Task]
  ]] =
    makeControllers.map(gatherRoutes)
}
