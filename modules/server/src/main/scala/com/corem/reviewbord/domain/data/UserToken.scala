package com.corem.reviewbord.domain.data

import zio.json.JsonCodec

final case class UserToken(
    email: String,
    accessToken: String,
    expires: Long
) derives JsonCodec
