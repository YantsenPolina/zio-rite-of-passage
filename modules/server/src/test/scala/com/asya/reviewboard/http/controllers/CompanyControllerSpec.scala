package com.asya.reviewboard.http.controllers

import com.asya.reviewboard.domain.data.Company
import com.asya.reviewboard.http.requests.CreateCompanyRequest
import com.asya.reviewboard.services.CompanyService
import com.asya.reviewboard.syntax.*
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*

object CompanyControllerSpec extends ZIOSpecDefault {
  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  private val asyaTheCatCompany = Company(1, "asya-the-cat", "Asya the Cat", "asyathecat.com")

  private val serviceStub = new CompanyService {
    override def create(request: CreateCompanyRequest): Task[Company] =
      ZIO.succeed(asyaTheCatCompany)

    override def getAll: Task[List[Company]] =
      ZIO.succeed(List(asyaTheCatCompany))

    override def getById(id: Long): Task[Option[Company]] =
      ZIO.succeed {
        if (id == 1) Some(asyaTheCatCompany)
        else None
      }

    override def getBySlug(slug: String): Task[Option[Company]] =
      ZIO.succeed {
        if (slug == asyaTheCatCompany.slug) Some(asyaTheCatCompany)
        else None
      }
  }

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
      test("Create a company.") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/companies")
            .body(CreateCompanyRequest("Asya the Cat", "asyathecat.com").toJson)
            .send(backendStub)
        } yield response.body

        program.assert { responseBody =>
          responseBody.toOption
            .flatMap(_.fromJson[Company].toOption)
            .contains(asyaTheCatCompany)
        }
      },
      test("Get all companies.") {
        val program = for {
          backendStub <- backendStubZIO(_.getAll)
          response <- basicRequest
            .get(uri"/companies")
            .send(backendStub)
        } yield response.body

        program.assert { responseBody =>
          responseBody.toOption
            .flatMap(_.fromJson[List[Company]].toOption)
            .contains(List(asyaTheCatCompany))
        }
      },
      test("Get the company by id.") {
        val program = for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/companies/1")
            .send(backendStub)
        } yield response.body

        program.assert { responseBody =>
          responseBody.toOption.flatMap(_.fromJson[Company].toOption).contains(asyaTheCatCompany)
        }
      }
    )
      .provide(ZLayer.succeed(serviceStub))
}
