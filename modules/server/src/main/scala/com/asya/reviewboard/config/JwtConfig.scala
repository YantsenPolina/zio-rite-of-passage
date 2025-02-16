package com.asya.reviewboard.config

case class JwtConfig(
    secret: String,
    ttl: Long
)
