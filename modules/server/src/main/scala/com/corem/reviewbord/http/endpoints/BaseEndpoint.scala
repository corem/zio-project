package com.corem.reviewbord.http.endpoints

import com.corem.reviewbord.domain.errors.HttpError
import sttp.tapir.*

trait BaseEndpoint {
  val baseEndpoint = endpoint
    .errorOut(statusCode and plainBody[String])
    .mapErrorOut[Throwable](HttpError.decode)(HttpError.encode)

  val secureBaseEndpoint = baseEndpoint.securityIn(auth.bearer[String]())
}
