package com.asya.reviewboard.domain.data

case class UserToken(
    email: String,
    token: String,
    expires: Long
)
