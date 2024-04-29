package com.corem.reviewbord.http.endpoints

import com.corem.reviewbord.domain.data.*
import com.corem.reviewbord.http.requests.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait CompanyEndpoints {
  val createEndpoint =
    endpoint
      .tag("companies")
      .name("create")
      .description("Create a listing for a company")
      .in("companies")
      .post
      .in(jsonBody[CreateCompanyRequest])
      .out(jsonBody[Company])

  val getAllEndpoint =
    endpoint
      .tag("companies")
      .name("getAll")
      .description("Get all company listings")
      .in("companies")
      .get
      .out(jsonBody[List[Company]])

  val getByIdEndpoint =
    endpoint
      .tag("companies")
      .name("getById")
      .description("Get company by its id")
      .in("companies" / path[String]("id"))
      .get
      .out(jsonBody[Option[Company]])
}
