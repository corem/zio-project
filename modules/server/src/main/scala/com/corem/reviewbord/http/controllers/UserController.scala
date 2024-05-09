package com.corem.reviewbord.http.controllers

import com.corem.reviewbord.domain.data.{UserId, UserToken}
import com.corem.reviewbord.domain.errors.UnauthorizedException
import com.corem.reviewbord.http.endpoints.UserEndpoints
import com.corem.reviewbord.http.responses.UserResponse
import com.corem.reviewbord.services.{JWTService, UserService}
import sttp.tapir.server.ServerEndpoint
import zio.*
import sttp.tapir.*

class UserController private (userService: UserService, jwtService: JWTService)
    extends BaseController
    with UserEndpoints {

  val create: ServerEndpoint[Any, Task] = createUserEndpoint.serverLogic { req =>
    userService.registerUser(req.email, req.password).map(user => UserResponse(user.email)).either
  }

  val updatePassword: ServerEndpoint[Any, Task] =
    updatePasswordEndpoint
      .serverSecurityLogic[UserId, Task] { token =>
        jwtService.verifyToken(token).either
      }
      .serverLogic { userId => req =>
        userService
          .updatePassword(req.email, req.oldPassword, req.newPassword)
          .map(user => UserResponse(user.email))
          .either
      }

  val delete: ServerEndpoint[Any, Task] = deleteEndpoint
    .serverSecurityLogic[UserId, Task] { token =>
      jwtService.verifyToken(token).either
    }
    .serverLogic { userId => req =>
      userService
        .deleteUser(req.email, req.password)
        .map(user => UserResponse(user.email))
        .either
    }

  val login: ServerEndpoint[Any, Task] = loginEndpoint.serverLogic { req =>
    userService.generateToken(req.email, req.password).someOrFail(UnauthorizedException).either
  }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    create,
    updatePassword,
    delete,
    login
  )
}

object UserController {
  val makeZIO = for {
    userService <- ZIO.service[UserService]
    jwtService  <- ZIO.service[JWTService]
  } yield new UserController(userService, jwtService)
}
