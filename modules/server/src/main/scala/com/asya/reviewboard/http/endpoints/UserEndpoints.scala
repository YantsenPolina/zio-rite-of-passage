package com.asya.reviewboard.http.endpoints

import com.asya.reviewboard.domain.data.UserToken
import com.asya.reviewboard.http.requests.{
  DeleteAccountRequest,
  LoginRequest,
  RegisterAccountRequest,
  UpdatePasswordRequest
}
import com.asya.reviewboard.http.responses.UserResponse
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait UserEndpoints extends BaseEndpoint {
  val createEndpoint = baseEndpoint
    .tag("users")
    .name("register")
    .description("Register a user account with username and password.")
    .in("users")
    .post
    .in(jsonBody[RegisterAccountRequest])
    .out(jsonBody[UserResponse])

  val updatePasswordEndpoint = secureBaseEndpoint
    .tag("users")
    .name("update password")
    .description("Update user's password.")
    .in("users" / "password")
    .put
    .in(jsonBody[UpdatePasswordRequest])
    .out(jsonBody[UserResponse])

  val deleteEndpoint = secureBaseEndpoint
    .tag("users")
    .name("delete")
    .description("Delete user's account.")
    .in("users")
    .delete
    .in(jsonBody[DeleteAccountRequest])
    .out(jsonBody[UserResponse])

  val loginEndpoint = baseEndpoint
    .tag("users")
    .name("login")
    .description("Log in and generate JWT token.")
    .in("users" / "login")
    .post
    .in(jsonBody[LoginRequest])
    .out(jsonBody[UserToken])
}
