package com.corem.reviewbord.http.requests

import com.corem.reviewbord.domain.data.Review
import zio.json.{DeriveJsonCodec, JsonCodec}

import java.time.Instant

final case class CreateReviewRequest(
    companyId: Long,
    management: Int,
    culture: Int,
    salary: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String
) {
  def toReview(id: Long, userId: Long) =
    Review(
      id = -1L,
      companyId = companyId,
      userId = userId,
      management = management,
      culture = culture,
      salary = salary,
      benefits = benefits,
      wouldRecommend = wouldRecommend,
      review = review,
      created = Instant.now(),
      updated = Instant.now()
    )
}

object CreateReviewRequest {
  given codec: JsonCodec[CreateReviewRequest] = DeriveJsonCodec.gen[CreateReviewRequest]
}
