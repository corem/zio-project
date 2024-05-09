package com.corem.reviewbord.config

final case class JWTConfig(
    secret: String,
    ttl: Long
)
