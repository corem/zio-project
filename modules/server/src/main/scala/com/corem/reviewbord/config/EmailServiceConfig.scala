package com.corem.reviewbord.config

final case class EmailServiceConfig(
    host: String,
    port: Int,
    user: String,
    password: String
)
