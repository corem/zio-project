package com.corem.reviewboard.services

import com.corem.reviewbord.config.JWTConfig
import com.corem.reviewbord.domain.data.User
import com.corem.reviewbord.services.{JWTService, JWTServiceLive}
import zio.*
import zio.test.*

object JWTServiceSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("JWTServiceSpec")(
      test("Create and validate Token") {
        for {
          service   <- ZIO.service[JWTService]
          userToken <- service.createToken(User(1L, "corem@core.com", "unimportant"))
          userId    <- service.verifyToken(userToken.accessToken)
        } yield assertTrue(
          userId.id == 1L &&
            userId.email == "corem@core.com"
        )
      }
    ).provide(JWTServiceLive.layer, ZLayer.succeed(JWTConfig("secret", 3600)))
}
