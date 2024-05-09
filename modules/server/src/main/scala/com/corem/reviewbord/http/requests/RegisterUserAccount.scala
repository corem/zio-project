package com.corem.reviewbord.http.requests

import zio.json.JsonCodec

final case class RegisterUserAccount(
    email: String,
    password: String
) derives JsonCodec
