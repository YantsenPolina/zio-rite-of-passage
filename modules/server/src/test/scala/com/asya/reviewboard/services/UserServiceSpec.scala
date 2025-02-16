package com.asya.reviewboard.services

import com.asya.reviewboard.domain.data.{User, UserId, UserToken}
import com.asya.reviewboard.repositories.UserRepository
import zio.test.{assertTrue, Spec, TestEnvironment, ZIOSpecDefault}
import zio.{Scope, Task, ZIO, ZLayer}

object UserServiceSpec extends ZIOSpecDefault {
  private val asya = User(1L, "asya@gmail.com", UserServiceLive.Hasher.generateHash("password123"))

  private val stubRepoLayer = ZLayer.succeed {
    new UserRepository {
      private val db = collection.mutable.Map[Long, User](1L -> asya)

      override def create(user: User): Task[User] =
        ZIO.succeed {
          db += (user.id -> user)
          user
        }

      override def update(id: Long, op: User => User): Task[User] =
        ZIO.attempt {
          val user = db(id)
          db += (id -> op(user))
          user
        }

      override def delete(id: Long): Task[User] =
        ZIO.attempt {
          val user = db(id)
          db -= id
          user
        }

      override def getById(id: Long): Task[Option[User]] = ZIO.succeed(db.get(id))

      override def getByEmail(email: String): Task[Option[User]] =
        ZIO.succeed(db.values.find(_.email == email))
    }
  }

  private val stubJwtLayer = ZLayer.succeed {
    new JwtService {
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.email, "token", Long.MaxValue))

      override def verifyToken(token: String): Task[UserId] =
        ZIO.succeed(UserId(asya.id, asya.email))
    }
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("UserServiceSpec")(
      test("Create and validate a user.") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.registerUser(asya.email, "password123")
          valid   <- service.verifyPassword(asya.email, "password123")
        } yield assertTrue(
          valid && user.email == asya.email
        )
      },
      test("Validate correct password.") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(asya.email, "password123")
        } yield assertTrue(valid)
      },
      test("Invalidate incorrect password.") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(asya.email, "wrongPassword")
        } yield assertTrue(!valid)
      },
      test("Invalidate non-existing user.") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword("someone@gmail.com", "somePassword")
        } yield assertTrue(!valid)
      },
      test("Update the password.") {
        for {
          service          <- ZIO.service[UserService]
          updatedUser      <- service.updatePassword(asya.email, "password123", "newPassword")
          oldPasswordValid <- service.verifyPassword(asya.email, "password123")
          newPasswordValid <- service.verifyPassword(asya.email, "newPassword")
        } yield assertTrue(!oldPasswordValid && newPasswordValid)
      },
      test("Delete a non-existing user.") {
        for {
          service <- ZIO.service[UserService]
          error   <- service.deleteUser("someone@gmail.com", "somePassword").flip
        } yield assertTrue(error.isInstanceOf[RuntimeException])
      },
      test("Delete the user with incorrect password.") {
        for {
          service <- ZIO.service[UserService]
          error   <- service.deleteUser(asya.email, "wrongPassword").flip
        } yield assertTrue(error.isInstanceOf[RuntimeException])
      },
      test("Delete the user.") {
        for {
          service     <- ZIO.service[UserService]
          deletedUser <- service.deleteUser(asya.email, "password123")
        } yield assertTrue(deletedUser.email == asya.email)
      }
    ).provide(
      UserServiceLive.layer,
      stubJwtLayer,
      stubRepoLayer
    )
}
