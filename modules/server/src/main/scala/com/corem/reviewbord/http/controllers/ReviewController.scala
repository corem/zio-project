package com.corem.reviewbord.http.controllers

import com.corem.reviewbord.domain.data.Review
import com.corem.reviewbord.http.endpoints.ReviewEndpoints
import com.corem.reviewbord.services.ReviewService
import sttp.tapir.server.ServerEndpoint
import zio.*

class ReviewController private (service: ReviewService)
    extends BaseController
    with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint.serverLogic { createReviewRequest =>
      service.create(createReviewRequest, -1L).either // TODO: Add UserId
    }

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic { id =>
      service.getById(id).either
    }

  val getByCompanyId: ServerEndpoint[Any, Task] =
    getByCompanyIdEndpoint.serverLogic { id =>
      service.getByCompanyId(id).either
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getById, getByCompanyId)
}

object ReviewController {
  val makeZIO = for {
    service <- ZIO.service[ReviewService]
  } yield new ReviewController(service)
}
