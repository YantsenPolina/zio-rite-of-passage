package com.asya.reviewboard.repositories

import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import zio.{Scope, ZIO, ZLayer}

import javax.sql.DataSource

trait RepositorySpec {
  val initScript: String

  // Spawn a Postgres instance on Docker just for the test.
  private def createPostgresContainer() = {
    val container: PostgreSQLContainer[Nothing] =
      PostgreSQLContainer("postgres").withInitScript(initScript)
    container.start()
    container
  }

  // Create a DataSource to connect to the Postgres.
  private def createDataSource(container: PostgreSQLContainer[Nothing]): DataSource = {
    val dataSource = new PGSimpleDataSource()
    dataSource.setURL(container.getJdbcUrl)
    dataSource.setUser(container.getUsername)
    dataSource.setPassword(container.getPassword)
    dataSource
  }

  // Use a DataSource (as a ZLayer) to build the Quill instance (as a ZLayer).
  val dataSourceLayer: ZLayer[Any & Scope, Throwable, DataSource] = ZLayer {
    for {
      container <- ZIO.acquireRelease(ZIO.attempt(createPostgresContainer()))(container =>
        ZIO.attempt(container.stop()).ignoreLogged
      )
      dataSource <- ZIO.attempt(createDataSource(container))
    } yield dataSource
  }
}
