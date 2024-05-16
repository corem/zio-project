package com.corem.reviewbord.domain.data

import zio.json.JsonCodec

final case class RecoveryToken(
    email: String,
    token: String,
    expires: Long
) derives JsonCodec
