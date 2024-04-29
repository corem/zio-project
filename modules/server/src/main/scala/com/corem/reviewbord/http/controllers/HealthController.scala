package com.corem.reviewbord.http.controllers

import com.corem.reviewbord.http.endpoints.HealthEndpoint
import sttp.tapir.server.ServerEndpoint
import zio.*

class HealthController private extends BaseController with HealthEndpoint {
  val health = healthEndpoint.serverLogicSuccess[Task](_ => ZIO.succeed("All Good !"))

  override val routes: List[ServerEndpoint[Any, Task]] = List(health)
}

object HealthController {
  val makeZIO = ZIO.succeed(new HealthController)
}
