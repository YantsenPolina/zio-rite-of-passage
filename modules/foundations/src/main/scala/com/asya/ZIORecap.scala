package com.asya

import zio.*

import scala.io.StdIn

object ZIORecap extends ZIOAppDefault {
  // ZIO is a data structure describing arbitrary computations (including side effects).
  // Effects are computations as values.

  private val meaningOfLife: ZIO[Any, Nothing, Int] = ZIO.succeed(42)
  val aFailure: ZIO[Any, String, Nothing] = ZIO.fail("Something went wrong.")
  val aSuspension: ZIO[Any, Throwable, Int] = ZIO.suspend(meaningOfLife)

  val improvedMeaningOfLife: ZIO[Any, Nothing, Int] = meaningOfLife.map(_ * 2)
  val printingMeaningOfLife: ZIO[Any, Nothing, Unit] = meaningOfLife.flatMap(m => ZIO.succeed(println(m)))

  private val smallProgram = for {
    _ <- Console.printLine("What is your name?")
    name <- ZIO.succeed(StdIn.readLine())
    _ <- Console.printLine(s"Welcome to ZIO, $name!")
  } yield ()

  private val anAttempt = ZIO.attempt {
    println("Trying something...")
    val string: String = null
    string.length
  }

  private val catchError = anAttempt.catchAll(e => ZIO.succeed("Returning some different value..."))
  private val catchSelective = anAttempt.catchSome {
    case e: RuntimeException => ZIO.succeed(s"Ignoring runtime exception: $e.")
    case _ => ZIO.succeed("Ignoring everything else...")
  }

  private val delayedValue = ZIO.sleep(1.second) *> Random.nextIntBetween(0, 100)
  private val aPair = for {
    a <- delayedValue
    b <- delayedValue
  } yield (a, b) // This takes 2 seconds.

  private val aPairParallel = for {
    fiberA <- delayedValue.fork // Returns some other effect which has a Fiber.
    fiberB <- delayedValue.fork
    a <- fiberA.join
    b <- fiberB.join
  } yield (a, b) // This takes 1 second.

  private val interruptedFiber = for {
    fiber <- delayedValue.map(println).onInterrupt(ZIO.succeed(println("I am interrupted!"))).fork
    _ <- ZIO.sleep(500.millis) *> ZIO.succeed(println("Cancelling fiber...")) *> fiber.interrupt
    _ <- fiber.join
  } yield ()

  private val ignoredInterruption = for {
    fiber <- ZIO.uninterruptible(delayedValue.map(println).onInterrupt(ZIO.succeed(println("I am interrupted!")))).fork
    _ <- ZIO.sleep(500.millis) *> ZIO.succeed(println("Cancelling fiber...")) *> fiber.interrupt
    _ <- fiber.join
  } yield ()

  private val aPairParallel_v2 = delayedValue.zipPar(delayedValue)
  private val randomTimes10 = ZIO.collectAllPar((1 to 10).map(_ => delayedValue))

  case class User(name: String, email: String)
  class UserSubscription(emailService: EmailService, userDatabase: UserDatabase) {
    def subscribeUser(user: User): Task[Unit] =
      for {
        _ <- emailService.email(user)
        _ <- userDatabase.insert(user)
        _ <- ZIO.succeed(s"User is subscribed: $user.")
      } yield ()
  }
  private object UserSubscription {
    val live: ZLayer[EmailService & UserDatabase, Nothing, UserSubscription] =
      ZLayer.fromFunction(new UserSubscription(_, _))
  }
  class EmailService {
    def email(user: User): Task[Unit] = ZIO.succeed(s"Sent email to user: $user.")
  }
  private object EmailService {
    val live: ZLayer[Any, Nothing, EmailService] = ZLayer.succeed(new EmailService)
  }
  class UserDatabase(connectionPool: ConnectionPool) {
    def insert(user: User): Task[Unit] = ZIO.succeed(s"User is inserted: $user.")
  }
  private object UserDatabase {
    val live: ZLayer[ConnectionPool, Nothing, UserDatabase] = ZLayer.fromFunction(new UserDatabase(_))
  }
  class ConnectionPool(numberOfConnections: Int) {
    def get: Task[Connection] = ZIO.succeed(Connection())
  }
  private object ConnectionPool {
    def live(numberOfConnections: Int): ZLayer[Any, Nothing, ConnectionPool] =
      ZLayer.succeed(ConnectionPool(numberOfConnections))
  }
  case class Connection()

  private def subscribe(user: User): ZIO[UserSubscription, Throwable, Unit] = for {
    subscription <- ZIO.service[UserSubscription]
    _ <- subscription.subscribeUser(user)
  } yield ()

  private val program = for {
    _ <- subscribe(User("Asya", "asya@gmail.com"))
    _ <- subscribe(User("Floki", "floki@gmail.com"))
  } yield ()

//  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = Console.printLine("Rock the JVM!")
//  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = smallProgram
//  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = interruptedFiber
//  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = ignoredInterruption
  override def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] = program.provide(
    ConnectionPool.live(10),
    UserDatabase.live,
    EmailService.live,
    UserSubscription.live
  )
}
