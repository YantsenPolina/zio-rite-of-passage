package com.asya.reviewboard.http.controllers

import com.asya.reviewboard.http.endpoints.ReviewEndpoints
import com.asya.reviewboard.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import zio.*

class ReviewController private (service: ReviewService)
    extends BaseController
    with ReviewEndpoints {
  val create: ServerEndpoint[Any, Task] =
    createEndpoint.serverLogicSuccess { request =>
      service.create(request, -1L) // TODO
    }

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogicSuccess { id =>
      service.getById(id)
    }

  val getByCompanyId: ServerEndpoint[Any, Task] =
    getByCompanyIdEndpoint.serverLogicSuccess { companyId =>
      service.getByCompanyId(companyId)
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getById, getByCompanyId)
}

object ReviewController {
  val makeZIO: URIO[ReviewService, ReviewController] =
    ZIO.service[ReviewService].map(service => ReviewController(service))
}
