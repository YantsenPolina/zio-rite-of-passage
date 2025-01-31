package com.asya.reviewboard.services

import com.asya.reviewboard.domain.data.Review
import com.asya.reviewboard.http.requests.CreateReviewRequest
import com.asya.reviewboard.repositories.ReviewRepository
import zio.test.{assertTrue, Spec, TestEnvironment, ZIOSpecDefault}
import zio.{Scope, Task, ZIO, ZLayer}

import java.time.Instant

object ReviewServiceSpec extends ZIOSpecDefault {
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

  private val badReview = Review(
    id = 2L,
    companyId = 1L,
    userId = 1L,
    management = 1,
    culture = 1,
    salary = 1,
    benefits = 1,
    wouldRecommend = 1,
    review = "All bad.",
    created = Instant.now(),
    updated = Instant.now()
  )

  private val stubRepoLayer = ZLayer.succeed {
    new ReviewRepository {
      override def create(review: Review): Task[Review] =
        ZIO.succeed(goodReview)

      override def update(id: Long, op: Review => Review): Task[Review] =
        getById(id)
          .someOrFail(new RuntimeException(s"Could not update review: missing id $id."))
          .map(op)

      override def delete(id: Long): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"Could not update review: missing id $id."))

      override def getById(id: Long): Task[Option[Review]] =
        ZIO.succeed {
          id match
            case 1 => Some(goodReview)
            case 2 => Some(badReview)
            case _ => None
        }

      override def getByCompanyId(companyId: Long): Task[List[Review]] =
        ZIO.succeed {
          if (companyId == 1) List(goodReview, badReview) else List()
        }

      override def getByUserId(userId: Long): Task[List[Review]] =
        ZIO.succeed {
          if (userId == 1) List(goodReview, badReview) else List()
        }
    }
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewServiceSpec")(
      test("Create a review.") {
        for {
          service <- ZIO.service[ReviewService]
          review <- service.create(
            CreateReviewRequest(
              companyId = goodReview.companyId,
              management = goodReview.management,
              culture = goodReview.culture,
              salary = goodReview.salary,
              benefits = goodReview.benefits,
              wouldRecommend = goodReview.wouldRecommend,
              review = goodReview.review
            ),
            userId = 1L
          )
        } yield assertTrue(
          review.companyId == goodReview.companyId &&
            review.management == goodReview.management &&
            review.culture == goodReview.culture &&
            review.salary == goodReview.salary &&
            review.benefits == goodReview.benefits &&
            review.wouldRecommend == goodReview.wouldRecommend &&
            review.review == goodReview.review
        )
      },
      test("Get the review by id.") {
        for {
          service        <- ZIO.service[ReviewService]
          review         <- service.getById(1L)
          reviewNotFound <- service.getById(999L)
        } yield assertTrue(
          review.contains(goodReview) &&
            reviewNotFound.isEmpty
        )
      },
      test("Get the review by company.") {
        for {
          service         <- ZIO.service[ReviewService]
          reviews         <- service.getByCompanyId(1L)
          reviewsNotFound <- service.getByCompanyId(999L)
        } yield assertTrue(
          reviews.toSet == Set(goodReview, badReview) &&
            reviewsNotFound.isEmpty
        )
      },
      test("Get the review by user.") {
        for {
          service         <- ZIO.service[ReviewService]
          reviews         <- service.getByUserId(1L)
          reviewsNotFound <- service.getByUserId(999L)
        } yield assertTrue(
          reviews.toSet == Set(goodReview, badReview) &&
            reviewsNotFound.isEmpty
        )
      }
    ).provide(ReviewServiceLive.layer, stubRepoLayer)
}
