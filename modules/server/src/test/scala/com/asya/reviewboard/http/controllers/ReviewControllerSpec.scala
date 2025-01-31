package com.asya.reviewboard.http.controllers

import com.asya.reviewboard.domain.data.Review
import com.asya.reviewboard.http.requests.CreateReviewRequest
import com.asya.reviewboard.services.ReviewService
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

import java.time.Instant

object ReviewControllerSpec extends ZIOSpecDefault {
  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  private val goodReview = Review(
    id = 1L,
    companyId = 1L,
    userId = 1L,
    management = 5,
    culture = 5,
    salary = 5,
    benefits = 5,
    wouldRecommend = 10,
    review = "All good.",
    created = Instant.now(),
    updated = Instant.now()
  )

  private val serviceStub = new ReviewService {
    override def create(request: CreateReviewRequest, userId: Long): Task[Review] =
      ZIO.succeed(goodReview)

    override def getById(id: Long): Task[Option[Review]] =
      ZIO.succeed {
        if (id == 1) Some(goodReview)
        else None
      }

    override def getByCompanyId(companyId: Long): Task[List[Review]] =
      ZIO.succeed {
        if (companyId == 1) List(goodReview)
        else List()
      }

    override def getByUserId(userId: Long): Task[List[Review]] =
      ZIO.succeed {
        if (userId == 1) List(goodReview)
        else List()
      }
  }

  private def backendStubZIO(endpointFunc: ReviewController => ServerEndpoint[Any, Task]) = for {
    controller <- ReviewController.makeZIO
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(endpointFunc(controller))
        .backend()
    )
  } yield backendStub

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewControllerSpec")(
      test("Create a review.") {
        val program = for {
          backendStub <- backendStubZIO(_.create)
          response <- basicRequest
            .post(uri"/reviews")
            .body(
              CreateReviewRequest(
                companyId = 1L,
                management = 5,
                culture = 5,
                salary = 5,
                benefits = 5,
                wouldRecommend = 10,
                review = "All good."
              ).toJson
            )
            .send(backendStub)
        } yield response.body

        program.assert { responseBody =>
          responseBody.toOption
            .flatMap(_.fromJson[Review].toOption)
            .contains(goodReview)
        }
      },
      test("Get the review by id.") {
        for {
          backendStub <- backendStubZIO(_.getById)
          response <- basicRequest
            .get(uri"/reviews/1")
            .send(backendStub)
          responseNotFound <- basicRequest
            .get(uri"/reviews/999")
            .send(backendStub)
        } yield assertTrue(
          response.body.toOption.flatMap(_.fromJson[Review].toOption).contains(goodReview) &&
            responseNotFound.body.toOption.flatMap(_.fromJson[Review].toOption).isEmpty
        )
      },
      test("Get the review by company.") {
        for {
          backendStub <- backendStubZIO(_.getByCompanyId)
          response <- basicRequest
            .get(uri"/reviews/company/1")
            .send(backendStub)
          responseNotFound <- basicRequest
            .get(uri"/reviews/company/999")
            .send(backendStub)
        } yield assertTrue(
          response.body.toOption
            .flatMap(_.fromJson[List[Review]].toOption)
            .contains(List(goodReview)) &&
            responseNotFound.body.toOption
              .flatMap(_.fromJson[List[Review]].toOption)
              .contains(List())
        )
      }
    )
      .provide(ZLayer.succeed(serviceStub))
}
