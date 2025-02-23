package com.asya.reviewboard.http.controllers

import com.asya.reviewboard.domain.data.UserId
import com.asya.reviewboard.domain.errors.UnauthorizedException
import com.asya.reviewboard.http.endpoints.UserEndpoints
import com.asya.reviewboard.http.responses.UserResponse
import com.asya.reviewboard.services.{JwtService, UserService}
import sttp.tapir.auth
import sttp.tapir.server.ServerEndpoint
import zio.{Task, URIO, ZIO}

class UserController private (userService: UserService, jwtService: JwtService)
    extends BaseController
    with UserEndpoints {
  val create: ServerEndpoint[Any, Task] =
    createEndpoint.serverLogic { request =>
      userService
        .registerUser(request.email, request.password)
        .map(user => UserResponse(user.email))
        .either
    }

  val updatePassword: ServerEndpoint[Any, Task] =
    updatePasswordEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { userId => request =>
        userService
          .updatePassword(request.email, request.oldPassword, request.newPassword)
          .map(user => UserResponse(user.email))
          .either
      }

  val delete: ServerEndpoint[Any, Task] =
    deleteEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { userId => request =>
        userService
          .deleteUser(request.email, request.password)
          .map(user => UserResponse(user.email))
          .either
      }

  val login: ServerEndpoint[Any, Task] =
    loginEndpoint.serverLogic { request =>
      userService
        .generateToken(request.email, request.password)
        .someOrFail(UnauthorizedException)
        .either
    }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, updatePassword, delete, login)
}

object UserController {
  val makeZIO: URIO[JwtService & UserService, UserController] =
    for {
      userService <- ZIO.service[UserService]
      jwtService  <- ZIO.service[JwtService]
    } yield UserController(userService, jwtService)
}
