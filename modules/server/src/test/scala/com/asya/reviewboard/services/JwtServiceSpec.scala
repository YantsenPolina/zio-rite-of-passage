package com.asya.reviewboard.services

import com.asya.reviewboard.config.JwtConfig
import com.asya.reviewboard.domain.data.User
import zio.test.{assertTrue, Spec, TestEnvironment, ZIOSpecDefault}
import zio.{Scope, ZIO, ZLayer}

object JwtServiceSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("JwtServiceSpec")(
      test("Create and validate a token.") {
        for {
          service   <- ZIO.service[JwtService]
          userToken <- service.createToken(User(1L, "asya@gmail.com", "password123"))
          userId    <- service.verifyToken(userToken.token)
        } yield assertTrue(
          userId.id == 1L &&
            userId.email == "asya@gmail.com"
        )
      }
    ).provide(
      JwtServiceLive.layer,
      ZLayer.succeed(JwtConfig("secret", 3600))
    )
}
