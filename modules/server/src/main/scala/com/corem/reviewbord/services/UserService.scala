package com.corem.reviewbord.services

import com.corem.reviewbord.domain.data.*
import com.corem.reviewbord.repositories.{RecoveryTokenRepository, UserRepository}
import zio.*

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

trait UserService {
  def registerUser(email: String, password: String): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
  def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User]
  def deleteUser(email: String, password: String): Task[User]
  def generateToken(email: String, password: String): Task[Option[UserToken]]
  def sendPasswordRecoveryToken(email: String): Task[Unit]
  def recoverPasswordFromToken(email: String, token: String, newPassword: String): Task[Boolean]
}

class UserServiceLive private (
    jwtService: JWTService,
    emailService: EmailService,
    repo: UserRepository,
    tokenRepo: RecoveryTokenRepository
) extends UserService {
  override def registerUser(email: String, password: String): Task[User] =
    repo.create(
      User(
        id = -1L,
        email = email,
        hashedPassword = UserServiceLive.Hasher.generateHash(password)
      )
    )
  override def verifyPassword(email: String, password: String): Task[Boolean] =
    for {
      existingUser <- repo
        .getByEmail(email)
      result <- existingUser match {
        case Some(user) =>
          ZIO
            .attempt(
              UserServiceLive.Hasher.validateHash(password, user.hashedPassword)
            )
            .orElseSucceed(false)
        case None => ZIO.succeed(false)
      }
    } yield result

  override def generateToken(email: String, password: String): Task[Option[UserToken]] =
    for {
      existingUser <- repo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"Cannot verify user $email: inexistant"))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      )
      maybeToken <- jwtService.createToken(existingUser).when(verified)
    } yield maybeToken

  override def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User] =
    for {
      existingUser <- repo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"Cannot verify user $email: inexistant"))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(oldPassword, existingUser.hashedPassword)
      )
      updatedUser <- repo
        .update(
          existingUser.id,
          user => user.copy(hashedPassword = UserServiceLive.Hasher.generateHash(newPassword))
        )
        .when(verified)
        .someOrFail(new RuntimeException(s"Cannot update password for $email"))
    } yield updatedUser

  override def deleteUser(email: String, password: String): Task[User] =
    for {
      existingUser <- repo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"Cannot verify user $email: inexistant"))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      )
      updatedUser <- repo
        .delete(existingUser.id)
        .when(verified)
        .someOrFail(new RuntimeException(s"Cannot delete user $email"))
    } yield updatedUser

  override def sendPasswordRecoveryToken(email: String): Task[Unit] =
    tokenRepo.getToken(email).flatMap {
      case Some(token) =>
        emailService.sendPasswordRecoveryEmail(email, token)
      case None => ZIO.unit
    }

  override def recoverPasswordFromToken(
      email: String,
      token: String,
      newPassword: String
  ): Task[Boolean] =
    for {
      existingUser <- repo.getByEmail(email).someOrFail(new RuntimeException("Non-existent user"))
      tokenIsValid <- tokenRepo.checkToken(email, token)
      result <- repo
        .update(
          existingUser.id,
          user => user.copy(hashedPassword = UserServiceLive.Hasher.generateHash(newPassword))
        )
        .when(tokenIsValid)
        .map(_.nonEmpty)
    } yield result
}

object UserServiceLive {
  val layer = ZLayer {
    for {
      jwtService   <- ZIO.service[JWTService]
      emailService <- ZIO.service[EmailService]
      repo         <- ZIO.service[UserRepository]
      tokenRepo    <- ZIO.service[RecoveryTokenRepository]
    } yield new UserServiceLive(jwtService, emailService, repo, tokenRepo)
  }

  object Hasher {
    private val PBKDF2_ALGORITHM: String = "PBKDF2WithHmacSHA512"
    private val PBKDF2_ITERATIONS: Int   = 1000
    private val SALT_BYTE_SIZE: Int      = 24
    private val HASH_BYTE_SIZE: Int      = 24
    private val skf: SecretKeyFactory    = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)

    private def pbkdf2(
        message: Array[Char],
        salt: Array[Byte],
        iterations: Int,
        nBytes: Int
    ): Array[Byte] = {
      val keySpec: PBEKeySpec = new PBEKeySpec(message, salt, iterations, nBytes * 8)
      skf.generateSecret(keySpec).getEncoded()
    }

    private def toHex(array: Array[Byte]): String =
      array.map(b => "%02X".format(b)).mkString

    private def fromHex(string: String): Array[Byte] = {
      string.sliding(2, 2).toArray.map { hexValue =>
        Integer.parseInt(hexValue, 16).toByte
      }
    }

    private def compareBytes(a: Array[Byte], b: Array[Byte]): Boolean = {
      val range = 0 until math.min(a.length, b.length)
      val diff = range.foldLeft(a.length ^ b.length) { case (acc, i) =>
        acc | (a(i) ^ b(i))
      }
      diff == 0
    }

    def generateHash(string: String): String = {
      val rng: SecureRandom = new SecureRandom()
      val salt: Array[Byte] = Array.ofDim[Byte](SALT_BYTE_SIZE)
      rng.nextBytes(salt)
      val hashBytes = pbkdf2(string.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
      s"$PBKDF2_ITERATIONS:${toHex(salt)}:${toHex(hashBytes)}"
    }

    def validateHash(string: String, hash: String): Boolean = {
      val hashSegments = hash.split(":")
      val nbIterations = hashSegments(0).toInt
      val salt         = fromHex(hashSegments(1))
      val validHash    = fromHex(hashSegments(2))
      val testHash     = pbkdf2(string.toCharArray(), salt, nbIterations, HASH_BYTE_SIZE)
      compareBytes(testHash, validHash)
    }
  }
}

object UserServiceDemo {
  def main(args: Array[String]) =
    println(UserServiceLive.Hasher.generateHash("corem.com"))
    println(
      UserServiceLive.Hasher.validateHash(
        "corem.com",
        "1000:1CCC79A4A9743CAF0B57E29E328FF14D3B5178204FBB2CC4:4701C25232433B49DC1E2AB55A9530CE383948B5D9DEDDCD"
      )
    )
}
