package com.asya

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.Server

import scala.collection.mutable

object TapirDemo extends ZIOAppDefault {
  private val simpleEndpoint = endpoint
    .tag("simple")
    .name("simple")
    .description("Simple endpoint.")
    .get // HTTP method.
    .in("simple") // Path.
    .out(plainBody[String]) // Output.
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

  private val simpleServerProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default // Can add configuration, for example, CORS.
    ).toHttp(simpleEndpoint)
  )

  private val database: mutable.Map[Long, Job] = mutable.Map(
    1L -> Job(1L, "Software Engineer", "asya.com", "Asya")
  )

  private val createEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("create")
    .description("Create a job.")
    .in("jobs")
    .post
    .in(jsonBody[CreateJobRequest])
    .out(jsonBody[Job])
    .serverLogicSuccess(request => ZIO.succeed {
      val newId = database.keys.max + 1
      val newJob = Job(newId, request.title, request.url, request.company)
      database += (newId -> newJob)
      newJob
    })

  private val getByIdEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("getById")
    .description("Get job by id.")
    .in("jobs" / path[Long]("id"))
    .get
    .out(jsonBody[Option[Job]])
    .serverLogicSuccess(id => ZIO.succeed(database.get(id)))

  private val getAllEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("getAll")
    .description("Get all jobs.")
    .in("jobs")
    .get
    .out(jsonBody[List[Job]])
    .serverLogicSuccess(_ => ZIO.succeed(database.values.toList))

  private val serverProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default
    ).toHttp(List(createEndpoint, getByIdEndpoint, getAllEndpoint))
  )

//  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
//    simpleServerProgram.provide(Server.default)

  override def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] =
    serverProgram.provide(Server.default)
}
