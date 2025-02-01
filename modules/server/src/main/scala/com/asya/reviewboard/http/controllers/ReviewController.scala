package com.asya.reviewboard.http.controllers

import com.asya.reviewboard.http.endpoints.ReviewEndpoints
import com.asya.reviewboard.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import zio.*

class ReviewController private (service: ReviewService)
    extends BaseController
    with ReviewEndpoints {
  val create: ServerEndpoint[Any, Task] =
    createEndpoint.serverLogic { request =>
      service.create(request, -1L).either // TODO
    }

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic { id =>
      service.getById(id).either
    }

  val getByCompanyId: ServerEndpoint[Any, Task] =
    getByCompanyIdEndpoint.serverLogic { companyId =>
      service.getByCompanyId(companyId).either
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getById, getByCompanyId)
}

object ReviewController {
  val makeZIO: URIO[ReviewService, ReviewController] =
    ZIO.service[ReviewService].map(service => ReviewController(service))
}
