package com.corem.reviewbord.http.controllers

import com.corem.reviewbord.http.endpoints.HealthEndpoint
import zio.*

class HealthController private extends HealthEndpoint {
  val health = healthEndpoint.serverLogicSuccess[Task](_ => ZIO.succeed("All Goodi !"))
}

object HealthController {
  val makeZIO = ZIO.succeed(new HealthController)
}
