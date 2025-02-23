package com.asya.reviewboard.http.requests

import zio.json.JsonCodec

final case class RegisterAccountRequest(
    email: String,
    password: String
) derives JsonCodec
