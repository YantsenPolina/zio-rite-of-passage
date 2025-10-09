package com.rockthejvm

import zio.*

import scala.io.StdIn

object ZIORecap extends ZIOAppDefault {
  // ZIO is a data structure describing arbitrary computations (including side effects).
  // Effects are computations as values.

  // Basics.
  private val meaningOfLife: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
  val aFailure: ZIO[Any, String, Nothing]           = ZIO.fail("Something went wrong.")
  // Suspension/delay.
  val aSuspension: ZIO[Any, Throwable, Int] = ZIO.suspend(meaningOfLife)

  private val improvedMeaningOfLife = meaningOfLife.map(_ * 2)
  private val printingMeaningOfLife = meaningOfLife.flatMap(mol => ZIO.succeed(println(mol)))
  private val smallProgram = for {
    _    <- Console.printLine("What is your name?")
    name <- ZIO.succeed(StdIn.readLine())
    _    <- Console.printLine(s"Welcome to ZIO, $name!")
  } yield ()

  // Error Handling.
  private val attempt: ZIO[Any, Throwable, Int] = ZIO.attempt {
    // Expression, which can throw.
    println("Trying something...")
    val string: String = null
    string.length
  }

  // Catch errors effectfully.
  private val catchError = attempt.catchAll(e => ZIO.succeed("Returning some different value..."))
  private val catchSelective = attempt.catchSome {
    case e: RuntimeException => ZIO.succeed(s"Ignoring RuntimeException: $e...")
    case _                   => ZIO.succeed("Ignoring everything else...")
  }

  // Fibers.
  private val delayedValue = ZIO.sleep(1.second) *> Random.nextIntBetween(0, 100)
  private val aPair = for {
    a <- delayedValue
    b <- delayedValue
  } yield (a, b) // This takes 2 seconds.

  private val aPairParallel = for {
    fiberA <- delayedValue.fork // Returns some other effect which has a Fiber.
    fiberB <- delayedValue.fork
    a      <- fiberA.join
    b      <- fiberB.join
  } yield (a, b) // This takes 1 second.

  private val interruptedFiber = for {
    fiber <- delayedValue.map(println).onInterrupt(ZIO.succeed(println("I am interrupted!"))).fork
    _     <- ZIO.sleep(500.millis) *> ZIO.succeed(println("Cancelling fiber...")) *> fiber.interrupt
    _     <- fiber.join
  } yield ()

  private val ignoredInterruption = for {
    fiber <- ZIO
      .uninterruptible(
        delayedValue.map(println).onInterrupt(ZIO.succeed(println("I am interrupted!")))
      )
      .fork
    _ <- ZIO.sleep(500.millis) *> ZIO.succeed(println("Cancelling fiber...")) *> fiber.interrupt
    _ <- fiber.join
  } yield ()

  private val aPairParallel_v2 = delayedValue.zipPar(delayedValue)

  private val randomTimes10 = ZIO.collectAllPar((1 to 10).map(_ => delayedValue))

  case class User(name: String, email: String)
  class UserSubscription(emailService: EmailService, userDatabase: UserDatabase) {
    def subscribeUser(user: User): Task[Unit] = for {
      _ <- emailService.email(user)
      _ <- userDatabase.insert(user)
      _ <- ZIO.succeed(s"Subscribed $user.")
    } yield ()
  }
  object UserSubscription {
    val live: ZLayer[EmailService & UserDatabase, Nothing, UserSubscription] =
      ZLayer.fromFunction((emailService, userDatabase) =>
        new UserSubscription(emailService, userDatabase)
      )
  }
  class EmailService {
    def email(user: User): Task[Unit] = ZIO.succeed(s"Emailed $user.")
  }
  object EmailService {
    val live: ZLayer[Any, Nothing, EmailService] =
      ZLayer.succeed(new EmailService)
  }
  class UserDatabase(connectionPool: ConnectionPool) {
    def insert(user: User): Task[Unit] = ZIO.succeed(s"Inserted $user.")
  }
  object UserDatabase {
    val live: ZLayer[ConnectionPool, Nothing, UserDatabase] =
      ZLayer.fromFunction(new UserDatabase(_))
  }
  class ConnectionPool(nConnections: Int) {
    def get: Task[Connection] = ZIO.succeed(Connection())
  }
  object ConnectionPool {
    def live(nConnections: Int): ZLayer[Any, Nothing, ConnectionPool] =
      ZLayer.succeed(ConnectionPool(nConnections))
  }
  case class Connection()

  private def subscribeUser(user: User): ZIO[UserSubscription, Throwable, Unit] = for {
    userSubscription <- ZIO.service[UserSubscription]
    _                <- userSubscription.subscribeUser(user)
  } yield ()

  private val program: ZIO[UserSubscription, Throwable, Unit] = for {
    _ <- subscribeUser(User("Asya", "asya@gmail.com"))
    _ <- subscribeUser(User("Floki", "floki@gmail.com"))
  } yield ()

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
//    Console.printLine("Rock the JVM!")
//    smallProgram
//    interruptedFiber
//    ignoredInterruption
    program.provide(
      ConnectionPool.live(10),
      UserDatabase.live,
      EmailService.live,
      UserSubscription.live
    )
}
