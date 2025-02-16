package com.asya.reviewboard.config

import com.typesafe.config.ConfigFactory
import zio.*
import zio.config.magnolia.{descriptor, Descriptor}
import zio.config.typesafe.TypesafeConfig

object Configs {
  def makeLayer[C](
      path: String
  )(using desc: Descriptor[C], tag: Tag[C]): ZLayer[Any, Throwable, C] =
    TypesafeConfig.fromTypesafeConfig(
      ZIO.attempt(ConfigFactory.load().getConfig(path)),
      descriptor[C]
    )
}
