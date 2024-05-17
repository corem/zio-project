package com.corem.reviewbord.http.endpoints

import com.corem.reviewbord.domain.data.*
import com.corem.reviewbord.http.requests.*
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.*

trait CompanyEndpoints extends BaseEndpoint {
  val createEndpoint =
    secureBaseEndpoint
      .tag("companies")
      .name("create")
      .description("Create a listing for a company")
      .in("companies")
      .post
      .in(jsonBody[CreateCompanyRequest])
      .out(jsonBody[Company])

  val getAllEndpoint =
    baseEndpoint
      .tag("companies")
      .name("getAll")
      .description("Get all company listings")
      .in("companies")
      .get
      .out(jsonBody[List[Company]])

  val getByIdEndpoint =
    baseEndpoint
      .tag("companies")
      .name("getById")
      .description("Get company by id")
      .in("companies" / path[String]("id"))
      .get
      .out(jsonBody[Option[Company]])
}
