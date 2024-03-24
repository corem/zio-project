package com.corem.reviewbord

import sttp.tapir.*
import sttp.tapir.server.ziohttp.*
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  val healthEndpoint = endpoint
    .tag("health")
    .name("health")
    .description("health check")
    .get
    .in("health")
    .out(plainBody[String])
    .serverLogicSuccess[Task](_ => ZIO.succeed("All Good!"))

  val serverProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default
    ).toHttp(healthEndpoint)
  )
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = serverProgram.provide(
    Server.default
  )
}
