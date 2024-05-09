package com.corem.reviewbord.services

import com.corem.reviewbord.domain.data.Review
import com.corem.reviewbord.http.requests.CreateReviewRequest
import com.corem.reviewbord.repositories.ReviewRepository
import zio.*

trait ReviewService {
  def create(createReviewRequest: CreateReviewRequest, userId: Long): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(id: Long): Task[List[Review]]
  def getByUserId(id: Long): Task[List[Review]]
//  def update(id: Long, op: Review => Review): Task[Review]
//  def delete(id: Long): Task[Review]
}

class ReviewServiceLive private (repo: ReviewRepository) extends ReviewService {
  override def create(createReviewRequest: CreateReviewRequest, userId: Long): Task[Review] =
    repo.create(createReviewRequest.toReview(-1L, userId))
  override def getById(id: Long): Task[Option[Review]] =
    repo.getById(id)
  override def getByCompanyId(id: Long): Task[List[Review]] =
    repo.getByCompanyId(id)
  override def getByUserId(id: Long): Task[List[Review]] =
    repo.getByUserId(id)
}

object ReviewServiceLive {
  val layer = ZLayer {
    for {
      repo <- ZIO.service[ReviewRepository]
    } yield new ReviewServiceLive(repo)
  }
}
