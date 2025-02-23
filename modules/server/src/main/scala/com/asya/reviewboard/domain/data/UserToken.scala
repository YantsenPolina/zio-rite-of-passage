package com.asya.reviewboard.domain.data

import zio.json.JsonCodec

case class UserToken(
    email: String,
    token: String,
    expires: Long
) derives JsonCodec
