package com.corem.reviewbord.http.controllers

import com.corem.reviewbord.domain.errors.HttpError
import com.corem.reviewbord.http.endpoints.HealthEndpoint
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.*
import zio.*

class HealthController private extends BaseController with HealthEndpoint {
  val health = healthEndpoint
    .serverLogicSuccess[Task](_ => ZIO.succeed("All Good !"))

  val error = errorEndpoint
    .serverLogic[Task](_ => ZIO.fail(new RuntimeException("Boom !")).either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(health, error)
}

object HealthController {
  val makeZIO = ZIO.succeed(new HealthController)
}
