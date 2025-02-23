package com.asya.reviewboard

import com.asya.reviewboard.config.{Configs, JwtConfig}
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
    _ <- Console.printLine("Welcome!")
  } yield ()

  override def run: ZIO[Any & (ZIOAppArgs & Scope), Any, Any] =
    serverProgram.provide(
      Server.default,
      // Configuration.
      Configs.makeLayer[JwtConfig]("asyathecat.jwt"),
      // Services.
//      CompanyService.dummyLayer
      CompanyServiceLive.layer,
      ReviewServiceLive.layer,
      UserServiceLive.layer,
      JwtServiceLive.layer,
      // Repositories.
      CompanyRepositoryLive.layer,
      ReviewRepositoryLive.layer,
      UserRepositoryLive.layer,
      // Other requirements.
      Repository.dataLayer
    )
}
