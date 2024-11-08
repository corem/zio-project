package com.corem.reviewbord

import com.corem.reviewbord.config.{Configs, JWTConfig}
import com.corem.reviewbord.http.HttpApi
import com.corem.reviewbord.repositories.{
  CompanyRepositoryLive,
  RecoveryTokenRepositoryLive,
  Repository,
  ReviewRepositoryLive,
  UserRepositoryLive
}
import com.corem.reviewbord.services.{
  CompanyServiceLive,
  EmailServiceLive,
  JWTServiceLive,
  ReviewServiceLive,
  UserServiceLive
}
import sttp.tapir.*
import sttp.tapir.server.ziohttp.*
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  val serverProgram = for {
    _         <- Console.printLine("Starting server...")
    endpoints <- HttpApi.endpointsZIO
    docEndpoints = SwaggerInterpreter().fromServerEndpoints(endpoints, "Reviewboard", "1.0.0")
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default
      ).toHttp(
        endpoints ::: docEndpoints
      )
    )
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    serverProgram.provide(
      Server.default,
      // Services
      CompanyServiceLive.layer,
      ReviewServiceLive.layer,
      UserServiceLive.layer,
      JWTServiceLive.configuredLayer,
      EmailServiceLive.configuredLayer,
      // Repositories
      CompanyRepositoryLive.layer,
      ReviewRepositoryLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokenRepositoryLive.configuredLayer,
      // Other requirements
      Repository.dataLayer
    )
}
