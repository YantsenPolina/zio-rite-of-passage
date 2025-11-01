package com.rockthejvm.reviewboard

import com.rockthejvm.reviewboard.http.controllers.HealthController
import sttp.tapir.*
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  private val serverProgram = for {
    controller <- HealthController.makeZIO
    server <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default
      ).toHttp(controller.health)
    )
    _ <- Console.printLine("Rock the JVM!")
  } yield ()

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    serverProgram.provide(
      Server.default
    )
}
