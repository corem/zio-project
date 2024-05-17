package com.corem.reviewbord.http.controllers

import com.corem.reviewbord.domain.data.{Review, UserId}
import com.corem.reviewbord.http.endpoints.ReviewEndpoints
import com.corem.reviewbord.services.{JWTService, ReviewService}
import sttp.tapir.server.ServerEndpoint
import zio.*

class ReviewController private (service: ReviewService, jwtService: JWTService)
    extends BaseController
    with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint
      .serverSecurityLogic[UserId, Task] { token =>
        jwtService.verifyToken(token).either
      }
      .serverLogic { userId => createReviewRequest =>
        service.create(createReviewRequest, userId.id).either
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
    service    <- ZIO.service[ReviewService]
    jwtService <- ZIO.service[JWTService]
  } yield new ReviewController(service, jwtService)
}
