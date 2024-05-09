package com.corem.reviewbord.domain.data

final case class User(
    id: Long,
    email: String,
    hashedPassword: String
) {
  def toUserId = UserId(id, email)
}

final case class UserId(
    id: Long,
    email: String
)
