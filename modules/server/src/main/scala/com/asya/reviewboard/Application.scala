package com.asya.reviewboard

import com.asya.reviewboard.http.HttpApi
import com.asya.reviewboard.repositories.*
import com.asya.reviewboard.services.*
import sttp.tapir.server.ziohttp.*
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {
  private val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    _ <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default
      ).toHttp(endpoints)
    )
    _ <- Console.printLine("Rock the JVM!")
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    serverProgram.provide(
      Server.default,
      // Services.
//      CompanyService.dummyLayer
      CompanyServiceLive.layer,
      // Repositories.
      CompanyRepositoryLive.layer,
      // Other requirements.
      Repository.dataLayer
    )
}
