package com.corem.reviewbord.http

import com.corem.reviewbord.http.controllers.{BaseController, CompanyController, HealthController}

object HttpApi {

  val endpointsZIO = makeControllers.map(gatherRoutes)

  def makeControllers = for {
    health    <- HealthController.makeZIO
    companies <- CompanyController.makeZIO
  } yield List(health, companies)

  def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)
}
