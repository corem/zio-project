package com.corem.reviewbord.http.requests

import zio.json.JsonCodec

final case class LoginRequest(
    email: String,
    password: String
) derives JsonCodec
