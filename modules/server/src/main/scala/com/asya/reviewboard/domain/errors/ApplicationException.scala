package com.asya.reviewboard.domain.errors

abstract class ApplicationException(mesaage: String) extends RuntimeException(mesaage)

case object UnauthorizedException extends ApplicationException("Unauthorized.")
