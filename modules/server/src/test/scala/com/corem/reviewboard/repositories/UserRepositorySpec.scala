package com.corem.reviewboard.repositories

import com.corem.reviewbord.domain.data.User
import com.corem.reviewbord.repositories.{Repository, UserRepository, UserRepositoryLive}
import zio.*
import zio.test.*
import com.corem.reviewboard.syntax.*

object UserRepositorySpec extends ZIOSpecDefault with RepositorySpec {
  private val myUser = User(1L, "corem@core.com", "hashedPassword")

  override val initScript: String = "sql/users.sql"

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserRepositorySpec")(
      test("Create User") {
        val program = for {
          repo <- ZIO.service[UserRepository]
          user <- repo.create(myUser)
        } yield user

        program.assert {
          case User(_, "corem@core.com", "hashedPassword") => true
          case _                                           => false
        }
      }
    ).provide(UserRepositoryLive.layer, dataSourceLayer, Repository.quillLayer, Scope.default)
}
