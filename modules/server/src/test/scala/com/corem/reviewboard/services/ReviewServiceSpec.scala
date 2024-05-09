package com.corem.reviewboard.services

import zio.*
import zio.test.*
import com.corem.reviewboard.syntax.*
import com.corem.reviewbord.http.requests.CreateReviewRequest
import com.corem.reviewbord.services.{ReviewService, ReviewServiceLive}
import com.corem.reviewbord.domain.data.Review
import com.corem.reviewbord.repositories.ReviewRepository

import java.time.Instant

object ReviewServiceSpec extends ZIOSpecDefault {

  val service = ZIO.serviceWithZIO[ReviewService]

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

  private val badReview = Review(
    id = 2L,
    companyId = 1L,
    userId = 1L,
    management = 1,
    culture = 1,
    salary = 1,
    benefits = 2,
    wouldRecommend = 2,
    review = "Horrible",
    created = Instant.now(),
    updated = Instant.now()
  )

  val stubRepoLayer = ZLayer.succeed(
    new ReviewRepository {
      val db = collection.mutable.Map[Long, Review]()

      override def create(review: Review): Task[Review] = ZIO.succeed(goodReview)

      override def getById(id: Long): Task[Option[Review]] =
        ZIO.succeed {
          id match {
            case 1 => Some(goodReview)
            case 2 => Some(badReview)
            case _ => None
          }
        }

      override def getByCompanyId(id: Long): Task[List[Review]] =
        ZIO.succeed {
          if (id == 1) List(goodReview, badReview)
          else List()
        }

      override def getByUserId(id: Long): Task[List[Review]] =
        ZIO.succeed {
          if (id == 1) List(goodReview, badReview)
          else List()
        }

      override def update(id: Long, op: Review => Review): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"id $id not found")).map(op)

      override def delete(id: Long): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"id $id not found"))
    }
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ReviewServiceSpec")(
      test("Create") {
        for {
          service <- ZIO.service[ReviewService]
          review <- service.create(
            CreateReviewRequest(
              companyId = goodReview.companyId,
              management = goodReview.management,
              culture = goodReview.culture,
              salary = goodReview.salary,
              benefits = goodReview.benefits,
              wouldRecommend = goodReview.wouldRecommend,
              review = goodReview.review
            ),
            userId = 1L
          )
        } yield assertTrue {
          review.companyId == goodReview.companyId &&
          review.management == goodReview.management &&
          review.culture == goodReview.culture &&
          review.salary == goodReview.salary &&
          review.benefits == goodReview.benefits &&
          review.wouldRecommend == goodReview.wouldRecommend &&
          review.review == goodReview.review
        }
      },
      test("Get by Id") {
        for {
          service        <- ZIO.service[ReviewService]
          review         <- service.getById(1L)
          reviewNotFound <- service.getById(999L)
        } yield assertTrue {
          review.contains(goodReview) &&
          reviewNotFound.isEmpty
        }
      },
      test("Get by CompanyId") {
        for {
          service        <- ZIO.service[ReviewService]
          review         <- service.getByCompanyId(1L)
          reviewNotFound <- service.getByCompanyId(999L)
        } yield assertTrue {
          review.toSet == Set(goodReview, badReview) &&
          reviewNotFound.isEmpty
        }
      },
      test("Get by UserId") {
        for {
          service        <- ZIO.service[ReviewService]
          review         <- service.getByUserId(1L)
          reviewNotFound <- service.getByUserId(999L)
        } yield assertTrue {
          review.toSet == Set(goodReview, badReview) &&
          reviewNotFound.isEmpty
        }
      }
    ).provide(
      ReviewServiceLive.layer,
      stubRepoLayer
    )
}
