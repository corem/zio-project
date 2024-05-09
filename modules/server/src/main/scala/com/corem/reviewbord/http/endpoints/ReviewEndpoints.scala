package com.corem.reviewbord.http.endpoints

import com.corem.reviewbord.domain.data.*
import com.corem.reviewbord.http.requests.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait ReviewEndpoints extends BaseEndpoint {
  val createEndpoint =
    baseEndpoint
      .tag("reviews")
      .name("create")
      .description("Create a review")
      .in("reviews")
      .post
      .in(jsonBody[CreateReviewRequest])
      .out(jsonBody[Review])

  val getByIdEndpoint =
    baseEndpoint
      .tag("reviews")
      .name("getById")
      .description("Get review by id")
      .in("reviews" / path[Long]("id"))
      .get
      .out(jsonBody[Option[Review]])

  val getByCompanyIdEndpoint =
    baseEndpoint
      .tag("reviews")
      .name("getByCompanyId")
      .description("Get review by company id")
      .in("reviews" / "company" / path[Long]("id"))
      .get
      .out(jsonBody[List[Review]])
}
