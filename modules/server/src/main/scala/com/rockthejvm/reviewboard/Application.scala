package com.rockthejvm.reviewboard

import com.rockthejvm.reviewboard.http.HttpApi
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  private val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    server <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default
      ).toHttp(endpoints)
    )
    _ <- Console.printLine("Rock the JVM!")
  } yield ()

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    serverProgram.provide(
      Server.default
    )
}
