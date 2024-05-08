package com.corem.reviewboard.repositories

import com.corem.reviewboard.repositories.CompanyRepositorySpec.dataSourceLayer
import com.corem.reviewbord.domain.data.Review
import com.corem.reviewbord.repositories.{Repository, ReviewRepository, ReviewRepositoryLive}
import zio.*
import zio.test.*
import com.corem.reviewboard.syntax.*

import java.time.Instant

object ReviewRepositorySpec extends ZIOSpecDefault with RepositorySpec {
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

  override val initScript: String = "sql/reviews.sql"
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ReviewRepositorySpec")(
      test("Create") {
        val program = for {
          repo   <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
        } yield review

        program.assert { review =>
          review.management == goodReview.management &&
          review.culture == goodReview.culture &&
          review.salary == goodReview.salary &&
          review.benefits == goodReview.benefits &&
          review.wouldRecommend == goodReview.wouldRecommend &&
          review.review == goodReview.review
        }
      },
      test("Get by Id(s)") {
        val program = for {
          repo               <- ZIO.service[ReviewRepository]
          review             <- repo.create(goodReview)
          fetchedById        <- repo.getById(goodReview.id)
          fetchedByCompanyId <- repo.getByCompanyId(goodReview.companyId)
          fetchedByUserId    <- repo.getByUserId(goodReview.userId)
        } yield (review, fetchedById, fetchedByCompanyId, fetchedByUserId)

        program.assert { case (review, fetchedById, fetchedByCompanyId, fetchedByUserId) =>
          fetchedById.contains(review) &&
          fetchedByCompanyId.contains(review) &&
          fetchedByUserId.contains(review)
        }
      },
      test("Get all") {
        val program = for {
          repo           <- ZIO.service[ReviewRepository]
          goodReview     <- repo.create(goodReview)
          badReview      <- repo.create(badReview)
          reviewsCompany <- repo.getByCompanyId(goodReview.companyId)
          reviewsUser    <- repo.getByCompanyId(goodReview.userId)
        } yield (goodReview, badReview, reviewsCompany, reviewsUser)

        program.assert { case (goodReview, badReview, reviewsCompany, reviewsUser) =>
          reviewsCompany.toSet == Set(goodReview, badReview) &&
          reviewsUser.toSet == Set(goodReview, badReview)
        }
      },
      test("Update") {
        val newReviewText = "Perfecto Totoro"
        val program = for {
          repo        <- ZIO.service[ReviewRepository]
          goodReview  <- repo.create(goodReview)
          updated     <- repo.update(goodReview.id, _.copy(review = newReviewText))
          fetchedById <- repo.getById(goodReview.id)
        } yield (goodReview, updated, fetchedById)

        program.assert { case (goodReview, updated, fetchedById) =>
          goodReview.id == updated.id &&
          goodReview.companyId == updated.companyId &&
          goodReview.userId == updated.userId &&
          goodReview.management == updated.management &&
          goodReview.culture == updated.culture &&
          goodReview.salary == updated.salary &&
          goodReview.benefits == updated.benefits &&
          goodReview.wouldRecommend == updated.wouldRecommend &&
          updated.review == newReviewText &&
          goodReview.review != newReviewText &&
          goodReview.created == updated.created &&
          goodReview.updated != updated.updated
        }
      },
      test("Delete") {
        val program = for {
          repo        <- ZIO.service[ReviewRepository]
          goodReview  <- repo.create(goodReview)
          _           <- repo.delete(goodReview.id)
          maybeReview <- repo.getById(goodReview.id)
        } yield maybeReview

        program.assert(_.isEmpty)
      }
    ) provide (ReviewRepositoryLive.layer, dataSourceLayer, Repository.quillLayer, Scope.default)
}
