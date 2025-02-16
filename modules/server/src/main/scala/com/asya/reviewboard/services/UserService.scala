package com.asya.reviewboard.services

import com.asya.reviewboard.domain.data.{User, UserToken}
import com.asya.reviewboard.repositories.UserRepository
import zio.{Task, ZIO, ZLayer}

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

trait UserService {
  def registerUser(email: String, password: String): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
  def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User]
  def deleteUser(email: String, password: String): Task[User]
  def generateToken(email: String, password: String): Task[Option[UserToken]]
}

class UserServiceLive private (jwtService: JwtService, repo: UserRepository) extends UserService {
  override def registerUser(email: String, password: String): Task[User] =
    repo.create(
      User(
        id = -1L,
        email = email,
        hashedPassword = UserServiceLive.Hasher.generateHash(password)
      )
    )

  override def verifyPassword(email: String, password: String): Task[Boolean] =
    for {
      existingUser <- repo.getByEmail(email)
      result <- existingUser match {
        case Some(user) =>
          ZIO
            .attempt(
              UserServiceLive.Hasher.validateHash(password, user.hashedPassword)
            )
            .orElseSucceed(false)
        case None => ZIO.succeed(false)
      }
    } yield result

  override def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User] =
    for {
      existingUser <- repo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"Cannot verify user $email: user not found."))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(oldPassword, existingUser.hashedPassword)
      )
      updatedUser <- repo
        .update(
          existingUser.id,
          user => user.copy(hashedPassword = UserServiceLive.Hasher.generateHash(newPassword))
        )
        .when(verified)
        .someOrFail(new RuntimeException(s"Could not update password for user $email."))
    } yield updatedUser

  override def deleteUser(email: String, password: String): Task[User] =
    for {
      existingUser <- repo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"Cannot verify user $email: user not found."))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      )
      deletedUser <- repo
        .delete(existingUser.id)
        .when(verified)
        .someOrFail(new RuntimeException(s"Could not delete user $email."))
    } yield deletedUser

  override def generateToken(email: String, password: String): Task[Option[UserToken]] =
    for {
      existingUser <- repo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"Cannot verify user $email: user not found."))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      )
      maybeToken <- jwtService.createToken(existingUser).when(verified)
    } yield maybeToken
}

object UserServiceLive {
  val layer: ZLayer[UserRepository & JwtService, Nothing, UserServiceLive] = ZLayer {
    for {
      jwtService <- ZIO.service[JwtService]
      userRepo   <- ZIO.service[UserRepository]
    } yield UserServiceLive(jwtService, userRepo)
  }

  object Hasher {
    private val PBKDF2_ALGORITHM: String           = "PBKDF2WithHmacSHA512"
    private val PBKDF2_ITERATIONS: Int             = 1000
    private val SALT_BYTE_SIZE: Int                = 24
    private val HASH_BYTES_SIZE: Int               = 24
    private val secretKeyFactory: SecretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)

    def generateHash(password: String): String = {
      val randomNumberGenerator: SecureRandom = new SecureRandom()
      val salt: Array[Byte]                   = Array.ofDim[Byte](SALT_BYTE_SIZE)
      randomNumberGenerator.nextBytes(salt) // Creates 24 random bytes.
      val hashBytes = pbkdf2(password.toCharArray, salt, PBKDF2_ITERATIONS, HASH_BYTES_SIZE)
      s"$PBKDF2_ITERATIONS:${toHex(salt)}:${toHex(hashBytes)}"
    }

    private def pbkdf2(
        message: Array[Char],
        salt: Array[Byte],
        iterations: Int,
        nBytes: Int
    ): Array[Byte] = {
      val keySpec: PBEKeySpec = new PBEKeySpec(message, salt, iterations, nBytes * 8)
      secretKeyFactory.generateSecret(keySpec).getEncoded
    }

    private def toHex(array: Array[Byte]): String =
      array.map(byte => "%02X".format(byte)).mkString

    def validateHash(password: String, hashedPassword: String): Boolean = {
      val hashSegments = hashedPassword.split(":")
      val nIterations  = hashSegments(0).toInt
      val salt         = fromHex(hashSegments(1))
      val validHash    = fromHex(hashSegments(2))
      val testHash     = pbkdf2(password.toCharArray, salt, nIterations, HASH_BYTES_SIZE)
      compareBytes(testHash, validHash)
    }

    private def fromHex(hash: String): Array[Byte] =
      hash.sliding(2, 2).toArray.map { hexValue =>
        Integer.parseInt(hexValue, 16).toByte
      }

    private def compareBytes(a: Array[Byte], b: Array[Byte]): Boolean = {
      val range = 0 until math.min(a.length, b.length)
      val diff = range.foldLeft(a.length ^ b.length) { case (acc, i) =>
        acc | (a(i) ^ b(i))
      }
      diff == 0
    }
  }
}

object UserServiceDemo {
  def main(args: Array[String]): Unit =
    println(UserServiceLive.Hasher.generateHash("asya"))
    println(
      UserServiceLive.Hasher.validateHash(
        "asya",
        "1000:E98E2416A889F33CFC11A5610E2D0A4C6C62FF28AC9BCA93:38605F34DD51AD78A4D0C4F2735446F29C6BB8DBA970EA7A"
      )
    )
}
