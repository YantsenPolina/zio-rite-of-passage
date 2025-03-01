package com.asya.reviewboard.integration

import com.asya.reviewboard.config.JwtConfig
import com.asya.reviewboard.domain.data.UserToken
import com.asya.reviewboard.http.controllers.UserController
import com.asya.reviewboard.http.requests.{
  DeleteAccountRequest,
  LoginRequest,
  RegisterAccountRequest,
  UpdatePasswordRequest
}
import com.asya.reviewboard.http.responses.UserResponse
import com.asya.reviewboard.repositories.{
  Repository,
  RepositorySpec,
  UserRepository,
  UserRepositoryLive
}
import com.asya.reviewboard.services.{JwtServiceLive, UserServiceLive}
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{basicRequest, SttpBackend, UriContext}
import sttp.model.Method
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.json.*
import zio.test.{assertTrue, Spec, TestEnvironment, ZIOSpecDefault}
import zio.{Scope, Task, ZIO, ZLayer}

object UserFlowSpec extends ZIOSpecDefault with RepositorySpec {
  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  override val initScript: String = "sql/integration.sql"

  private def backendStubZIO = for {
    controller <- UserController.makeZIO
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointsRunLogic(controller.routes)
        .backend()
    )
  } yield backendStub

  extension [A: JsonCodec](backend: SttpBackend[Task, Nothing]) {
    def sendRequest[B: JsonCodec](
        method: Method,
        path: String,
        payload: A,
        maybeToken: Option[String] = None
    ): Task[Option[B]] =
      basicRequest
        .method(method, uri"$path")
        .body(payload.toJson)
        .auth
        .bearer(maybeToken.getOrElse(""))
        .send(backend)
        .map(_.body)
        .map(_.toOption.flatMap(payload => payload.fromJson[B].toOption))

    def postRequest[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.POST, path, payload)

    def postAuthRequest[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.POST, path, payload, Some(token))

    def putRequest[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload)

    def putAuthRequest[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, Some(token))

    def deleteRequest[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload)

    def deleteAuthRequest[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, Some(token))
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserFlowSpec")(
      test("Create a user.") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub
            .postRequest[UserResponse](
              "/users",
              RegisterAccountRequest(email = "asya@gmail.com", password = "password123")
            )
        } yield assertTrue(maybeResponse.contains(UserResponse("asya@gmail.com")))
      },
      test("Create a user and log in.") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub
            .postRequest[UserResponse](
              "/users",
              RegisterAccountRequest(email = "asya@gmail.com", password = "password123")
            )
          maybeToken <- backendStub
            .postRequest[UserToken]("/users/login", LoginRequest("asya@gmail.com", "password123"))
        } yield assertTrue(maybeToken.exists(_.email == "asya@gmail.com"))
      },
      test("Change the password.") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub
            .postRequest[UserResponse](
              "/users",
              RegisterAccountRequest(email = "asya@gmail.com", password = "password123")
            )
          userToken <- backendStub
            .postRequest[UserToken]("/users/login", LoginRequest("asya@gmail.com", "password123"))
            .someOrFail(new RuntimeException("Authentication failed."))
          _ <- backendStub
            .putAuthRequest[UserResponse](
              "/users/password",
              UpdatePasswordRequest(
                email = "asya@gmail.com",
                oldPassword = "password123",
                newPassword = "newPassword123"
              ),
              userToken.token
            )
          maybeOldToken <- backendStub
            .postRequest[UserToken]("/users/login", LoginRequest("asya@gmail.com", "password123"))
          maybeNewToken <- backendStub
            .postRequest[UserToken](
              "/users/login",
              LoginRequest("asya@gmail.com", "newPassword123")
            )
        } yield assertTrue(
          maybeOldToken.isEmpty && maybeNewToken.nonEmpty
        )
      },
      test("Delete the user.") {
        for {
          backendStub <- backendStubZIO
          userRepo    <- ZIO.service[UserRepository]
          maybeResponse <- backendStub
            .postRequest[UserResponse](
              "/users",
              RegisterAccountRequest(email = "asya@gmail.com", password = "password123")
            )
          maybeOldUser <- userRepo.getByEmail("asya@gmail.com")
          userToken <- backendStub
            .postRequest[UserToken]("/users/login", LoginRequest("asya@gmail.com", "password123"))
            .someOrFail(new RuntimeException("Authentication failed."))
          _ <- backendStub
            .deleteAuthRequest[UserResponse](
              "/users",
              DeleteAccountRequest(
                email = "asya@gmail.com",
                password = "password123"
              ),
              userToken.token
            )
          userRepo  <- ZIO.service[UserRepository]
          maybeUser <- userRepo.getByEmail("asya@gmail.com")
        } yield assertTrue(maybeOldUser.exists(_.email == "asya@gmail.com") && maybeUser.isEmpty)
      }
    ).provide(
      UserServiceLive.layer,
      JwtServiceLive.layer,
      UserRepositoryLive.layer,
      Repository.quillLayer,
      dataSourceLayer,
      ZLayer.succeed(JwtConfig("secret", 3600)),
      Scope.default
    )
}
