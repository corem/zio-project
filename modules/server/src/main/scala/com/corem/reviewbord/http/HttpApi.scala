package com.corem.reviewbord.http

import com.corem.reviewbord.http.controllers.{
  BaseController,
  CompanyController,
  HealthController,
  ReviewController,
  UserController
}

object HttpApi {

  val endpointsZIO = makeControllers.map(gatherRoutes)

  def makeControllers = for {
    health    <- HealthController.makeZIO
    companies <- CompanyController.makeZIO
    reviews   <- ReviewController.makeZIO
    users     <- UserController.makeZIO
  } yield List(health, companies, reviews, users)

  def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)
}
