package com.corem.reviewbord

import com.corem.reviewbord.http.controllers.HealthController
import sttp.tapir.*
import sttp.tapir.server.ziohttp.*
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  val serverProgram = for {
    controller <- HealthController.makeZIO
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default
      ).toHttp(controller.health)
    )
    _ <- Console.printLine("Server Debug!")
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = serverProgram.provide(
    Server.default
  )
}
