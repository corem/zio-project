package com.corem.reviewboard.http.controllers

import com.corem.reviewboard.syntax.*
import com.corem.reviewbord.domain.data.Review
import com.corem.reviewbord.http.controllers.ReviewController
import com.corem.reviewbord.http.requests.CreateReviewRequest
import com.corem.reviewbord.services.ReviewService
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*

import java.time.Instant

object ReviewControllerSpec extends ZIOSpecDefault {
  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val goodReview = Review(
    id = 1L,
    companyId = 1L,
    userId = 1L,
    management = 5,
    culture = 5,
    salary = 5,
    benefits = 5,
    wouldRecommend = 5,
    review = "Awesome possum",
    created = Instant.now(),
    updated = Instant.now()
  )

  private val serviceStub = new ReviewService {
    override def create(createReviewRequest: CreateReviewRequest, userId: Long): Task[Review] =
      ZIO.succeed(goodReview)

    override def getById(id: Long): Task[Option[Review]] =
      ZIO.succeed {
        if (id == 1L) Some(goodReview)
        else None
      }

    override def getByCompanyId(id: Long): Task[List[Review]] =
      ZIO.succeed {
        if (id == 1L) List(goodReview)
        else List()
      }

    override def getByUserId(id: Long): Task[List[Review]] =
      ZIO.succeed {
        if (id == 1L) List(goodReview)
        else List()
      }
  }

  private def backendStubZIO(endpointFun: ReviewController => ServerEndpoint[Any, Task]) =
    for {
      controller <- ReviewController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointRunLogic(endpointFun(controller))
          .backend()
      )
    } yield backendStub

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ReviewControllerSpec")(
      test("Post Review") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/reviews")
            .body(
              CreateReviewRequest(
                companyId = 1L,
                management = 5,
                culture = 5,
                salary = 5,
                benefits = 5,
                wouldRecommend = 5,
                review = "Awesome possum"
              ).toJson
            )
            .send(backendStub)
        } yield response.body

        program.assert { respBody =>
          respBody.toOption
            .flatMap(_.fromJson[Review].toOption)
            .contains(goodReview)
        }
      },
      test("Get by id") {
        for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/reviews/1")
            .send(backendStub)
          responseNotFound <- basicRequest
            .get(uri"/reviews/999")
            .send(backendStub)
        } yield assertTrue {
          response.body.toOption
            .flatMap(_.fromJson[Review].toOption)
            .contains(goodReview) &&
          responseNotFound.body.toOption
            .flatMap(_.fromJson[Review].toOption)
            .isEmpty
        }
      },
      test("Get by company id") {
        for {
          backendStub <- backendStubZIO(_.getByCompanyId)
          response <- basicRequest
            .get(uri"/reviews/company/1")
            .send(backendStub)
          responseNotFound <- basicRequest
            .get(uri"/reviews/company/999")
            .send(backendStub)
        } yield assertTrue {
          response.body.toOption
            .flatMap(_.fromJson[List[Review]].toOption)
            .contains(List(goodReview)) &&
          responseNotFound.body.toOption
            .flatMap(_.fromJson[List[Review]].toOption)
            .contains(List())
        }
      }
    ).provide(ZLayer.succeed(serviceStub))
}
