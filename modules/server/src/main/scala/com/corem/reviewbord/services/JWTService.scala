package com.corem.reviewbord.services

import com.corem.reviewbord.domain.data.*
import zio.*
import com.auth0.jwt.*
import com.auth0.jwt.JWTVerifier.BaseVerification
import com.auth0.jwt.algorithms.Algorithm
import com.corem.reviewbord.config.{Configs, JWTConfig}

trait JWTService {
  def createToken(user: User): Task[UserToken]
  def verifyToken(token: String): Task[UserId]
}

class JWTServiceLive(jwtConfig: JWTConfig, clock: java.time.Clock) extends JWTService {
  private val ISSUER         = "corem.com"
  private val algorithm      = Algorithm.HMAC512(jwtConfig.secret)
  private val CLAIM_USERNAME = "username"
  private val verifier: JWTVerifier =
    JWT
      .require(algorithm)
      .withIssuer(ISSUER)
      .asInstanceOf[BaseVerification]
      .build(clock)

  override def createToken(user: User): Task[UserToken] =
    for {
      now <- ZIO.attempt(clock.instant())
      expiration = now.plusSeconds(jwtConfig.ttl)
      jwtToken <- ZIO.attempt(
        JWT
          .create()
          .withIssuer(ISSUER)
          .withIssuedAt(now)
          .withExpiresAt(expiration) // 30 Days
          .withSubject(user.id.toString)
          .withClaim(CLAIM_USERNAME, user.email)
          .sign(algorithm)
      )
    } yield UserToken(user.email, jwtToken, expiration.getEpochSecond)

  override def verifyToken(token: String): Task[UserId] =
    for {
      decoded <- ZIO.attempt(verifier.verify(token))
      userId <- ZIO.attempt(
        UserId(decoded.getSubject.toLong, decoded.getClaim(CLAIM_USERNAME).asString())
      )

    } yield userId
}

object JWTServiceLive {
  val layer = ZLayer {
    for {
      jwtConfig <- ZIO.service[JWTConfig]
      clock     <- Clock.javaClock
    } yield new JWTServiceLive(jwtConfig, clock)
  }

  val configuredLayer =
    Configs.makeConfigLayer[JWTConfig]("corem.jwt") >>> layer
}

object JWTServiceDemo extends ZIOAppDefault {

  val program = for {
    service   <- ZIO.service[JWTService]
    userToken <- service.createToken(User(1L, "corem@core.com", "unimportant"))
    _         <- Console.printLine(userToken)
    userId    <- service.verifyToken(userToken.accessToken)
    _         <- Console.printLine(userId)
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.provide(
      JWTServiceLive.layer,
      Configs.makeConfigLayer[JWTConfig]("corem.jwt")
    )
}
