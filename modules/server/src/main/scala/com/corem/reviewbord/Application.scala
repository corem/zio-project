package com.corem.reviewbord

import com.corem.reviewbord.http.HttpApi
import com.corem.reviewbord.repositories.{CompanyRepositoryLive, Repository}
import com.corem.reviewbord.services.CompanyServiceLive
import sttp.tapir.*
import sttp.tapir.server.ziohttp.*
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default
      ).toHttp(endpoints)
    )
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    serverProgram.provide(
      Server.default,
      // Services
      CompanyServiceLive.layer,
      // Repositories
      CompanyRepositoryLive.layer,
      // Other requirements
      Repository.dataLayer
    )
}
