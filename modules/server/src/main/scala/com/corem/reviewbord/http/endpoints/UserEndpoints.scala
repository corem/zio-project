package com.corem.reviewbord.http.endpoints

import com.corem.reviewbord.domain.data.UserToken
import com.corem.reviewbord.http.requests.*
import com.corem.reviewbord.http.responses.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*
trait UserEndpoints extends BaseEndpoint {
  val createUserEndpoint =
    baseEndpoint
      .tag("users")
      .name("register")
      .description("Register a user account with username and password")
      .in("users")
      .post
      .in(jsonBody[RegisterUserAccount])
      .out(jsonBody[UserResponse])

  val updatePasswordEndpoint =
    secureBaseEndpoint
      .tag("users")
      .name("updatePassword")
      .description("Update user password")
      .in("users" / "password")
      .put
      .in(jsonBody[UpdatePasswordRequest])
      .out(jsonBody[UserResponse])

  val deleteEndpoint =
    secureBaseEndpoint
      .tag("users")
      .name("deleteAccount")
      .description("Delete user account")
      .in("users")
      .delete
      .in(jsonBody[DeleteAccountRequest])
      .out(jsonBody[UserResponse])

  val loginEndpoint =
    baseEndpoint
      .tag("users")
      .name("login")
      .description("Log in and generate JWT Token")
      .in("users" / "login")
      .post
      .in(jsonBody[LoginRequest])
      .out(jsonBody[UserToken])
}
