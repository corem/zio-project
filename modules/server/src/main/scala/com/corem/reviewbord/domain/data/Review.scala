package com.corem.reviewbord.domain.data

import java.time.Instant
import zio.json.{DeriveJsonCodec, JsonCodec}

final case class Review(
    id: Long,
    companyId: Long,
    userId: Long,
    management: Int,
    culture: Int,
    salary: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String,
    created: Instant,
    updated: Instant
)

object Review {
  given codec: JsonCodec[Review] = DeriveJsonCodec.gen[Review]
}
