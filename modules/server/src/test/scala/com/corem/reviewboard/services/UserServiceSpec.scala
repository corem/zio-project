package com.corem.reviewboard.services

import com.corem.reviewbord.domain.data.{User, UserId, UserToken}
import com.corem.reviewbord.repositories.{RecoveryTokenRepository, UserRepository}
import com.corem.reviewbord.services.{EmailService, JWTService, UserService, UserServiceLive}
import zio.*
import zio.test.*
object UserServiceSpec extends ZIOSpecDefault {

  private val testUser = User(
    1L,
    "corem@core.com",
    "1000:1CCC79A4A9743CAF0B57E29E328FF14D3B5178204FBB2CC4:4701C25232433B49DC1E2AB55A9530CE383948B5D9DEDDCD"
  )

  val stubRepoLayer = ZLayer.succeed {
    new UserRepository {
      val db = collection.mutable.Map[Long, User](1L -> testUser)

      override def create(user: User): Task[User] = ZIO.succeed {
        db += (user.id -> user)
        user
      }

      override def getById(id: Long): Task[Option[User]] = ZIO.succeed(db.get(id))

      override def getByEmail(email: String): Task[Option[User]] =
        ZIO.succeed(db.values.find(_.email == email))

      override def update(id: Long, op: User => User): Task[User] = ZIO.attempt {
        val newUser = op(db(id))
        db += (newUser.id -> newUser)
        newUser
      }

      override def delete(id: Long): Task[User] = ZIO.attempt {
        val user = db(id)
        db -= id
        user
      }
    }
  }

  val stubTokenRepoLayer = ZLayer.succeed {
    new RecoveryTokenRepository {
      val db = collection.mutable.Map[String, String]()

      override def getToken(email: String): Task[Option[String]] = ZIO.attempt {
        val token = util.Random.alphanumeric.take(8).mkString.toUpperCase
        db += (email -> token)
        Some(token)
      }

      override def checkToken(email: String, token: String): Task[Boolean] =
        ZIO.succeed(db.get(email).filter(_ == token).nonEmpty)
    }
  }

  val stubEmailsLayer = ZLayer.succeed {
    new EmailService {
      override def sendEmail(to: String, subject: String, content: String): Task[Unit] = ZIO.unit
    }
  }

  val stubJwtLayer = ZLayer.succeed {
    new JWTService {
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.email, "Access", Long.MaxValue))

      override def verifyToken(token: String): Task[UserId] =
        ZIO.succeed(UserId(testUser.id, testUser.email))
    }
  }
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserServiceSpec")(
      test("Create and validate a user") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.registerUser(testUser.email, "corem.com")
          valid   <- service.verifyPassword(testUser.email, "corem.com")
        } yield assertTrue(valid && user.email == testUser.email)
      },
      test("Validate credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(testUser.email, "corem.com")
        } yield assertTrue(valid)
      },
      test("Invalidate incorrect credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(testUser.email, "wrongpassword")
        } yield assertTrue(!valid)
      },
      test("Invalidate non existing User") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword("nonexisting@mail.com", "wrongpassword")
        } yield assertTrue(!valid)
      },
      test("Update password") {
        for {
          service  <- ZIO.service[UserService]
          valid    <- service.updatePassword(testUser.email, "corem.com", "newPassword")
          oldValid <- service.verifyPassword(testUser.email, "corem.com")
          newValid <- service.verifyPassword(testUser.email, "newPassword")
        } yield assertTrue(newValid && !oldValid)
      },
      test("Delete non existing user") {
        for {
          service <- ZIO.service[UserService]
          error   <- service.deleteUser("nonexisting@mail.com", "wrongpassword").flip
        } yield assertTrue(error.isInstanceOf[RuntimeException])
      },
      test("Delete with incorrect credentials") {
        for {
          service <- ZIO.service[UserService]
          error   <- service.deleteUser(testUser.email, "wrongpassword").flip
        } yield assertTrue(error.isInstanceOf[RuntimeException])
      },
      test("Delete existing user") {
        for {
          service     <- ZIO.service[UserService]
          deletedUser <- service.deleteUser(testUser.email, "corem.com")
        } yield assertTrue(deletedUser.email == testUser.email)
      }
    ).provide(
      UserServiceLive.layer,
      stubJwtLayer,
      stubRepoLayer,
      stubEmailsLayer,
      stubTokenRepoLayer
    )
}
