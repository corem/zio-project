package com.corem.reviewboard.http.controllers

import com.corem.reviewboard.syntax.*
import com.corem.reviewbord.domain.data.Company
import com.corem.reviewbord.http.controllers.CompanyController
import com.corem.reviewbord.http.requests.CreateCompanyRequest
import com.corem.reviewbord.services.CompanyService
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*

object CompanyControllerSpec extends ZIOSpecDefault {

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val corem = Company(1, "corem-corp", "Corem Corp", "core@corem.com")

  private val serviceStub = new CompanyService {
    override def create(createCompanyRequest: CreateCompanyRequest): Task[Company] =
      ZIO.succeed(corem)

    override def getAll: Task[List[Company]] =
      ZIO.succeed(List(corem))

    override def getById(id: Long): Task[Option[Company]] =
      ZIO.succeed {
        if (id == 1) Some(corem)
        else None
      }

    override def getBySlug(slug: String): Task[Option[Company]] =
      ZIO.succeed {
        if (slug == corem.slug) Some(corem)
        else None
      }
  }

  private def backendStubZIO(endpointFun: CompanyController => ServerEndpoint[Any, Task]) =
    for {
      controller <- CompanyController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointRunLogic(endpointFun(controller))
          .backend()
      )
    } yield backendStub

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyControllerSpec")(
      test("Post Company") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/companies")
            .body(CreateCompanyRequest("My Company", "corem.com").toJson)
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption
            .flatMap(_.fromJson[Company].toOption)
            .contains(corem)
        }
      },
      test("Get all Companies") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption
            .flatMap(_.fromJson[List[Company]].toOption)
            .contains(List(corem))
        }
      },
      test("Get by id") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption
            .flatMap(_.fromJson[Company].toOption)
            .contains(corem)
        }
      }
    )
      .provide(ZLayer.succeed(serviceStub))
}
