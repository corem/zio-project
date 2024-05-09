package com.corem.reviewbord.domain.data

final case class User(
    id: Long,
    email: String,
    hashedPassword: String
)
