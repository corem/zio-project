package com.corem.reviewbord.repositories

import com.corem.reviewbord.config.{Configs, RecoveryTokenConfig}
import com.corem.reviewbord.domain.data.RecoveryToken
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*
trait RecoveryTokenRepository {
  def getToken(email: String): Task[Option[String]]
  def checkToken(email: String, token: String): Task[Boolean]
}

class RecoveryTokenRepositoryLive private (
    tokenConfig: RecoveryTokenConfig,
    quill: Quill.Postgres[SnakeCase],
    userRepo: UserRepository
) extends RecoveryTokenRepository {
  import quill.*

  inline given schema: SchemaMeta[RecoveryToken]  = schemaMeta[RecoveryToken]("recovery_tokens")
  inline given insMeta: InsertMeta[RecoveryToken] = insertMeta[RecoveryToken]()
  inline given upMeta: UpdateMeta[RecoveryToken]  = updateMeta[RecoveryToken](_.email)

  private val tokenDuration = 600000 // Config

  private def randomUppercaseString(len: Int): Task[String] =
    ZIO.succeed(scala.util.Random.alphanumeric.take(len).mkString.toUpperCase)

  private def findToken(email: String): Task[Option[String]] =
    run(query[RecoveryToken].filter(_.email == lift(email))).map(_.headOption.map(_.token))
  private def replaceToken(email: String): Task[String] =
    for {
      token <- randomUppercaseString(8)
      _ <- run(
        query[RecoveryToken]
          .updateValue(
            lift(RecoveryToken(email, token, java.lang.System.currentTimeMillis() + tokenDuration))
          )
          .returning(r => r)
      )
    } yield token
  private def generateToken(email: String): Task[String] =
    for {
      token <- randomUppercaseString(8)
      _ <- run(
        query[RecoveryToken]
          .insertValue(
            lift(RecoveryToken(email, token, java.lang.System.currentTimeMillis() + tokenDuration))
          )
          .returning(r => r)
      )
    } yield token
  private def makeFreshToken(email: String): Task[String] =
    findToken(email).flatMap {
      case Some(_) => replaceToken(email)
      case None    => generateToken(email)
    }

  override def getToken(email: String): Task[Option[String]] = userRepo.getByEmail(email).flatMap {
    case None    => ZIO.none
    case Some(_) => makeFreshToken(email).map(Some(_))
  }

  override def checkToken(email: String, token: String): Task[Boolean] =
    run(query[RecoveryToken].filter(r => r.email == lift(email) && r.token == lift(token)))
      .map(_.nonEmpty)
}

object RecoveryTokenRepositoryLive {
  val layer = ZLayer {
    for {
      config   <- ZIO.service[RecoveryTokenConfig]
      quill    <- ZIO.service[Quill.Postgres[SnakeCase.type]]
      userRepo <- ZIO.service[UserRepository]
    } yield RecoveryTokenRepositoryLive(config, quill, userRepo)
  }

  val configuredLayer =
    Configs.makeConfigLayer[RecoveryTokenConfig]("corem.recoverytoken") >>> layer
}
