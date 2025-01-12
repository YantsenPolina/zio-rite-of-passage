package com.asya.reviewboard.http.controllers

import com.asya.reviewboard.domain.data.Company
import com.asya.reviewboard.http.requests.CreateCompanyRequest
import com.asya.reviewboard.syntax.*
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.generic.auto.*
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*
import sttp.tapir.server.ServerEndpoint

object CompanyControllerSpec extends ZIOSpecDefault {
  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  private def backendStubZIO(endpointFunc: CompanyController => ServerEndpoint[Any, Task]) = for {
     controller <- CompanyController.makeZIO
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
          .whenServerEndpointRunLogic(endpointFunc(controller))
          .backend()
      )
  } yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyControllerSpec")(
      test("create company") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/companies")
            .body(CreateCompanyRequest("Rock the JVM", "rockthejvm.com").toJson)
            .send(backendStub)
        } yield response.body

        program.assert { responseBody =>
          responseBody.toOption.flatMap(_.fromJson[Company].toOption)
            .contains(Company(1, "rock-the-jvm", "Rock the JVM", "rockthejvm.com"))
        }
      },

      test("get all companies") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        } yield response.body

        program.assert { responseBody =>
          responseBody.toOption.flatMap(_.fromJson[List[Company]].toOption)
            .contains(List())
        }
      },

      test("get company by id") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        } yield response.body

        program.assert { responseBody =>
          responseBody.toOption.flatMap(_.fromJson[Company].toOption)
            .isEmpty
        }
      }
    )
}
