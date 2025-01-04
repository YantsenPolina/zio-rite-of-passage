package com.asya

import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

object QuillDemo extends ZIOAppDefault {
  private val program = for {
    repo <- ZIO.service[JobRepository]
    _ <- repo.create(Job(-1L, "Software Engineer", "asya.com", "Asya"))
    _ <- repo.create(Job(-1L, "QA Engineer", "asya.com", "Asya"))
  } yield ()

  override def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] =
    program.provide(
      JobRepositoryLive.layer,
      Quill.Postgres.fromNamingStrategy(SnakeCase), // Quill instance.
      Quill.DataSource.fromPrefix("mydbconf") // Reads the configuration section in application.conf and spins up a data source.
    )
}

trait JobRepository {
  def create(job: Job): Task[Job]
  def update (id: Long, op: Job => Job): Task[Job]
  def delete(id: Long): Task[Job]
  def getById(id: Long): Task[Option[Job]]
  def get: Task[List[Job]]
}

class JobRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends JobRepository {
  import quill.*

  inline given schema: SchemaMeta[Job] = schemaMeta[Job]("jobs") // Specify the table name.
  inline given insMeta: InsertMeta[Job] = insertMeta[Job](_.id) // Columns to be excluded in insert statements.
  inline given updMeta: UpdateMeta[Job] = updateMeta[Job](_.id) // Columns to be excluded in update statements.

  override def create(job: Job): Task[Job] =
    run {
      query[Job]
        .insertValue(lift(job))
        .returning(j => j)
    }

  override def update(id: Long, op: Job => Job): Task[Job] = for {
    current <- getById(id).someOrFail(new RuntimeException(s"Could not update: missing key $id."))
    updated <- run {
      query[Job]
        .filter(_.id == lift(id))
        .updateValue(lift(op(current)))
        .returning(j => j)
    }
  } yield updated

  override def delete(id: Long): Task[Job] =
    run {
      query[Job]
        .filter(_.id == lift(id))
        .delete
        .returning(j => j)
    }

  override def getById(id: Long): Task[Option[Job]] =
    run {
      query[Job]
        .filter(_.id == lift(id))
    }.map(_.headOption)

  override def get: Task[List[Job]] = run(query[Job])
}

object JobRepositoryLive {
  val layer: ZLayer[Quill.Postgres[SnakeCase], Nothing, JobRepositoryLive] = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase]].map(quill => JobRepositoryLive(quill))
  }
}
