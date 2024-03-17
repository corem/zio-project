package com.corem

import zio.*

import java.io.IOException
import scala.io.StdIn

object ZIORecap extends ZIOAppDefault {

  val meaningOfLife: ZIO[Any, Nothing, Int] = ZIO.succeed(42)

  val aFailure: ZIO[Any, String, Nothing] = ZIO.fail("Error !")

  val aSuspension: ZIO[Any, Throwable, Int] = ZIO.suspend(meaningOfLife)

  val improvedMeaningOfLife: ZIO[Any, Nothing, Int] = meaningOfLife.map(_ * 2)
  val printingMeaningOfLife: ZIO[Any, Nothing, Unit] =
    meaningOfLife.flatMap(mol => ZIO.succeed(println(mol)))

  val smallProgram: ZIO[Any, IOException, Unit] =
    for {
      _    <- Console.printLine("What's your name ?")
      name <- ZIO.succeed(StdIn.readLine())
      _    <- Console.printLine(s"Welcome to ZIO, $name")
    } yield ()

  val anAttempt: ZIO[Any, Throwable, Int] = ZIO.attempt {
    println("Trying something")
    val string: String = null
    string.length
  }

  val catchError = anAttempt.catchAll(e => ZIO.succeed(s"Returning some different value"))
  val catchSelective = anAttempt.catchSome {
    case e: RuntimeException => ZIO.succeed(s"Ignoring runtime exception: $e")
    case _                   => ZIO.succeed("Ignoring everything else")
  }

  val delayedValue = ZIO.sleep(1.second) *> Random.nextIntBetween(0, 100)
  val aPair = for {
    a <- delayedValue
    b <- delayedValue
  } yield (a, b) // Takes 2 seconds

  val aPairPar = for {
    fibA <- delayedValue.fork
    fibB <- delayedValue.fork
    a    <- fibA.join
    b    <- fibB.join
  } yield (a, b)

  val interruptedFiber = for {
    fib <- delayedValue.map(println).onInterrupt(ZIO.succeed(println("I'm interrupted !"))).fork
    _   <- ZIO.sleep(500.millis) *> ZIO.succeed(println("Cancelling Fiber")) *> fib.interrupt
    _   <- fib.join
  } yield ()

  val ignoredInterruption = for {
    fib <- ZIO
      .uninterruptible(
        delayedValue.map(println).onInterrupt(ZIO.succeed(println("I'm interrupted !")))
      )
      .fork
    _ <- ZIO.sleep(500.millis) *> ZIO.succeed(println("Cancelling Fiber")) *> fib.interrupt
    _ <- fib.join
  } yield ()

  val aPairParV2 = delayedValue.zipPar(delayedValue)
  val randomx10  = ZIO.collectAllPar((1 to 10).map(_ => delayedValue)) // "Traverse"

  case class User(name: String, email: String)

  class UserSubscription(emailService: EmailService, userDatabase: UserDatabase) {
    def subscribeUser(user: User): Task[Unit] =
      for {
        _ <- emailService.email(user)
        _ <- userDatabase.insert(user)
        _ <- ZIO.succeed(s"Subscribed $user")
      } yield ()

  }

  object UserSubscription {
    val live: ZLayer[EmailService with UserDatabase, Nothing, UserSubscription] =
      ZLayer.fromFunction((emailS, userD) => new UserSubscription(emailS, userD))
  }

  class EmailService {
    def email(user: User): Task[Unit] = ZIO.succeed(s"Emailed $user")
  }

  object EmailService {
    val live: ZLayer[Any, Nothing, EmailService] =
      ZLayer.succeed(new EmailService)
  }

  class UserDatabase(connectionPool: ConnectionPool) {
    def insert(user: User): Task[Unit] = ZIO.succeed(s"Inserted $user")
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

  def subscribe(user: User): ZIO[UserSubscription, Throwable, Unit] = for {
    sub <- ZIO.service[UserSubscription]
    _   <- sub.subscribeUser(user)
  } yield ()

  val program = for {
    _ <- subscribe(User("Corem", "core@corem.com"))
    _ <- subscribe(User("Gato", "gato@corem.com"))
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = program.provide(
    ConnectionPool.live(10),
    UserDatabase.live,
    EmailService.live,
    UserSubscription.live
  )
}
