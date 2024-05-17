package com.corem.reviewboard.integration

import com.corem.reviewboard.repositories.RepositorySpec
import com.corem.reviewbord.config.{JWTConfig, RecoveryTokenConfig}
import com.corem.reviewbord.domain.data.UserToken
import com.corem.reviewbord.http.controllers.UserController
import com.corem.reviewbord.http.requests.{
  DeleteAccountRequest,
  ForgotPasswordRequest,
  LoginRequest,
  RecoverPasswordRequest,
  RegisterUserAccount,
  UpdatePasswordRequest
}
import com.corem.reviewbord.http.responses.UserResponse
import com.corem.reviewbord.repositories.{
  RecoveryTokenRepositoryLive,
  Repository,
  UserRepository,
  UserRepositoryLive
}
import com.corem.reviewbord.services.{EmailService, JWTServiceLive, UserServiceLive}
import sttp.client3.{basicRequest, SttpBackend, UriContext}
import sttp.client3.testing.SttpBackendStub
import sttp.model.Method
import sttp.monad.MonadError
import sttp.tapir.ztapir.RIOMonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import zio.test.{assertTrue, Spec, TestEnvironment, ZIOSpecDefault}
import zio.*
import zio.json.*

object UserFlowSpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String       = "sql/integration.sql"
  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private def backendStubZIO =
    for {
      controller <- UserController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointsRunLogic(controller.routes)
          .backend()
      )
    } yield backendStub

  extension [A: JsonCodec](backend: SttpBackend[Task, Nothing]) {
    def sendRequest[B: JsonCodec](
        method: Method,
        path: String,
        payload: A,
        maybeToken: Option[String] = None
    ): Task[Option[B]] =
      basicRequest
        .method(method, uri"$path")
        .body(
          payload.toJson
        )
        .auth
        .bearer(maybeToken.getOrElse(""))
        .send(backend)
        .map(_.body)
        .map(_.toOption.flatMap(payload => payload.fromJson[B].toOption))

    def post[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.POST, path, payload, None)

    def postAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.POST, path, payload, Some(token))

    def postNoResponse(path: String, payload: A): Task[Unit] =
      basicRequest.method(Method.POST, uri"$path").body(payload.toJson).send(backend).unit

    def put[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, None)

    def putAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, Some(token))

    def delete[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, None)

    def deleteAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, Some(token))
  }

  class EmailServiceProbe extends EmailService {
    val db = collection.mutable.Map[String, String]()

    override def sendEmail(to: String, subject: String, content: String): Task[Unit] = ZIO.unit

    override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] =
      ZIO.succeed(db += (to -> token))

    def probeToken(email: String): Task[Option[String]] = ZIO.succeed(db.get(email))
  }

  val emailServiceLayer: ZLayer[Any, Nothing, EmailServiceProbe] =
    ZLayer.succeed(new EmailServiceProbe)

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserFlowSpec")(
      test("Create user") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub.post[UserResponse](
            "/users",
            RegisterUserAccount("core@corem.com", "password")
          )
        } yield assertTrue {
          maybeResponse.contains(UserResponse("core@corem.com"))
        }
      },
      test("Create and log in") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub
            .post[UserResponse]("/users", RegisterUserAccount("core@corem.com", "password"))
          maybeToken <- backendStub
            .post[UserResponse]("/users/login", LoginRequest("core@corem.com", "password"))
        } yield assertTrue(
          maybeToken.exists(_.email == "core@corem.com")
        )
      },
      test("Update password") {
        for {
          backendStub <- backendStubZIO
          maybeResponse <- backendStub
            .post[UserResponse]("/users", RegisterUserAccount("core@corem.com", "password"))
          userToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("core@corem.com", "password"))
            .someOrFail(new RuntimeException("Authentication Failed"))
          _ <- backendStub
            .putAuth[UserResponse](
              "/users/password",
              UpdatePasswordRequest("core@corem.com", "password", "newpassword"),
              userToken.accessToken
            )
          maybeOldToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("core@corem.com", "password"))
          maybeNewToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("core@corem.com", "newpassword"))
        } yield assertTrue(
          maybeOldToken.isEmpty && maybeNewToken.nonEmpty
        )
      },
      test("Delete user") {
        for {
          backendStub <- backendStubZIO
          userRepo    <- ZIO.service[UserRepository]
          maybeResponse <- backendStub
            .post[UserResponse]("/users", RegisterUserAccount("core@corem.com", "password"))
          maybeOldUser <- userRepo.getByEmail("core@corem.com")
          userToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("core@corem.com", "password"))
            .someOrFail(new RuntimeException("Authentication Failed"))
          _ <- backendStub
            .deleteAuth[UserResponse](
              "/users",
              DeleteAccountRequest("core@corem.com", "password"),
              userToken.accessToken
            )
          maybeUser <- userRepo.getByEmail("core@corem.com")
        } yield assertTrue(
          maybeOldUser.exists(_.email == "core@corem.com") && maybeUser.isEmpty
        )
      },
      test("Recover password flow") {
        for {
          backendStub <- backendStubZIO
          _ <- backendStub
            .post[UserResponse]("/users", RegisterUserAccount("core@corem.com", "password"))
          _ <- backendStub.postNoResponse("/users/forgot", ForgotPasswordRequest("core@corem.com"))
          emailServiceProbe <- ZIO.service[EmailServiceProbe]
          token <- emailServiceProbe
            .probeToken("core@corem.com")
            .someOrFail(new RuntimeException("Token was not emailed"))
          _ <- backendStub.postNoResponse(
            "/users/recover",
            RecoverPasswordRequest("core@corem.com", token, "newpassword")
          )
          maybeOldToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("core@corem.com", "password"))
          maybeNewToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("core@corem.com", "newpassword"))
        } yield assertTrue(
          maybeOldToken.isEmpty && maybeNewToken.nonEmpty
        )
      }
    ).provide(
      UserServiceLive.layer,
      JWTServiceLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokenRepositoryLive.layer,
      emailServiceLayer,
      Repository.quillLayer,
      dataSourceLayer,
      ZLayer.succeed(JWTConfig("secret", 3600)),
      ZLayer.succeed(RecoveryTokenConfig(24 * 3600)),
      Scope.default
    )
}
