package com.asya.reviewboard.domain.data

case class User(
    id: Long,
    email: String,
    hashedPassword: String
) {
  def toUserId: UserId = UserId(id, email)
}

final case class UserId(
    id: Long,
    email: String
)
