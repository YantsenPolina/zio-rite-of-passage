package com.asya.reviewboard.services

import com.asya.reviewboard.config.{Configs, JwtConfig}
import com.asya.reviewboard.domain.data.{User, UserId, UserToken}
import com.auth0.jwt.JWTVerifier.BaseVerification
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.{JWT, JWTVerifier}
import zio.*

import java.time.Instant

trait JwtService {
  def createToken(user: User): Task[UserToken]
  def verifyToken(token: String): Task[UserId]
}

class JwtServiceLive(jwtConfig: JwtConfig, clock: java.time.Clock) extends JwtService {
  private val ISSUER         = "asyathecat.com"
  private val CLAIM_USERNAME = "username"
  private val algorithm      = Algorithm.HMAC512(jwtConfig.secret)
  private val verifier = JWT
    .require(algorithm)
    .withIssuer(ISSUER)
    .asInstanceOf[BaseVerification]
    .build(clock)

  override def createToken(user: User): Task[UserToken] =
    for {
      now        <- ZIO.attempt(clock.instant)
      expiration <- ZIO.succeed(now.plusSeconds(jwtConfig.ttl))
      token <- ZIO.attempt(
        JWT
          .create()
          .withIssuer(ISSUER)
          .withIssuedAt(now)
          .withExpiresAt(expiration)
          .withSubject(user.id.toString)
          .withClaim(CLAIM_USERNAME, user.email)
          .sign(algorithm)
      )
    } yield UserToken(
      email = user.email,
      token = token,
      expires = expiration.getEpochSecond
    )

  override def verifyToken(token: String): Task[UserId] =
    for {
      decoded <- ZIO.attempt(
        verifier.verify(token)
      )
      userId <- ZIO.attempt(
        UserId(
          id = decoded.getSubject.toLong,
          email = decoded.getClaim(CLAIM_USERNAME).asString()
        )
      )
    } yield userId
}

object JwtServiceLive {
  val layer: URLayer[JwtConfig, JwtServiceLive] = ZLayer {
    for {
      jwtConfig <- ZIO.service[JwtConfig]
      clock     <- Clock.javaClock
    } yield new JwtServiceLive(jwtConfig, clock)
  }

  val configuredLayer: TaskLayer[JwtServiceLive] =
    Configs.makeLayer[JwtConfig]("asyathecat.jwt") >>> layer
}

object JwtServiceDemo extends ZIOAppDefault {
  private val program = for {
    service   <- ZIO.service[JwtService]
    userToken <- service.createToken(User(1L, "asya@gmail.com", "asya"))
    _         <- Console.printLine(userToken)
    userId    <- service.verifyToken(userToken.token)
    _         <- Console.printLine(userId.toString)
  } yield ()

  override def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] =
    program.provide(
      JwtServiceLive.configuredLayer
    )
}
