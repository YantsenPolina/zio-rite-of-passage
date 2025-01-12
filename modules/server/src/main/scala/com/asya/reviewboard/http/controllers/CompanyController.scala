package com.asya.reviewboard.http.controllers

import com.asya.reviewboard.http.endpoints.CompanyEndpoints
import com.asya.reviewboard.domain.data.Company
import com.asya.reviewboard.services.CompanyService
import sttp.tapir.server.ServerEndpoint
import zio.*

class CompanyController private (service: CompanyService)
    extends BaseController
    with CompanyEndpoints {
  val create: ServerEndpoint[Any, Task] =
    createEndpoint.serverLogicSuccess { request =>
      service.create(request)
    }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogicSuccess(_ => service.getAll)

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogicSuccess { id =>
      ZIO
        .attempt(id.toLong)
        .flatMap(service.getById)
        .catchSome { case _: NumberFormatException =>
          service.getBySlug(id)
        }
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CompanyController {
  val makeZIO: URIO[CompanyService, CompanyController] = for {
    service <- ZIO.service[CompanyService]
  } yield new CompanyController(service)
}
