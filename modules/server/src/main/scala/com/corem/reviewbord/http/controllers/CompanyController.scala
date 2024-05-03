package com.corem.reviewbord.http.controllers

import com.corem.reviewbord.domain.data.Company
import com.corem.reviewbord.http.endpoints.CompanyEndpoints
import com.corem.reviewbord.services.CompanyService
import sttp.tapir.server.ServerEndpoint
import zio.*

class CompanyController private (service: CompanyService)
    extends BaseController
    with CompanyEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint.serverLogicSuccess { createCompanyRequest =>
      service.create(createCompanyRequest)
    }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogicSuccess { _ => service.getAll }

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogicSuccess { id =>
      ZIO
        .attempt(id.toLong)
        .flatMap(service.getById)
        .catchSome { case _: java.lang.NumberFormatException =>
          service.getBySlug(id)
        }
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CompanyController {
  val makeZIO = for {
    service <- ZIO.service[CompanyService]
  } yield new CompanyController(service)
}
