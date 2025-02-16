package com.asya.reviewboard.repositories

import com.asya.reviewboard.domain.data.User
import com.asya.reviewboard.syntax.assert
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault}
import zio.{Scope, ZIO, ZLayer}

import javax.sql.DataSource

object UserRepositorySpec extends ZIOSpecDefault with RepositorySpec {
  override val initScript: String = "sql/users.sql"

  private val user = User(1L, "asya@gmail.com", "password")

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserRepositorySpec")(
      test("Create a user.") {
        val program = for {
          repo <- ZIO.service[UserRepository]
          user <- repo.create(user)
        } yield user

        program.assert {
          case User(_, "asya@gmail.com", "password") => true
          case _                                     => false
        }
      },
      test("Get the user by id and email.") {
        val program = for {
          repo           <- ZIO.service[UserRepository]
          user           <- repo.create(user)
          fetchedById    <- repo.getById(user.id)
          fetchedByEmail <- repo.getByEmail(user.email)
        } yield (user, fetchedById, fetchedByEmail)

        program.assert { case (user, fetchedById, fetchedByEmail) =>
          fetchedById.contains(user) && fetchedByEmail.contains(user)
        }
      },
      test("Update the user.") {
        val program = for {
          repo        <- ZIO.service[UserRepository]
          user        <- repo.create(user)
          updated     <- repo.update(user.id, _.copy(hashedPassword = "password123"))
          fetchedById <- repo.getById(user.id)
        } yield (updated, fetchedById)

        program.assert { case (updated, fetchedById) =>
          fetchedById.contains(updated)
        }
      },
      test("Delete the user.") {
        val program = for {
          repo        <- ZIO.service[UserRepository]
          user        <- repo.create(user)
          _           <- repo.delete(user.id)
          fetchedById <- repo.getById(user.id)
        } yield fetchedById

        program.assert(_.isEmpty)
      }
    ).provide(
      UserRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
}
