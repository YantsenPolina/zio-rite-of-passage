package com.asya.reviewboard.repositories

import com.asya.reviewboard.domain.data.Review
import com.asya.reviewboard.syntax.assert
import zio.test.{assertTrue, Spec, TestEnvironment, ZIOSpecDefault}
import zio.{Scope, ZIO, ZLayer}

import java.time.Instant
import javax.sql.DataSource

object ReviewRepositorySpec extends ZIOSpecDefault with RepositorySpec {
  override val initScript: String = "sql/reviews.sql"

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

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ReviewRepositorySpec")(
      test("Create a review.") {
        val program = for {
          repo   <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
        } yield review

        program.assert { review =>
          review.management == goodReview.management &&
          review.culture == goodReview.culture &&
          review.salary == goodReview.salary &&
          review.benefits == goodReview.benefits &&
          review.wouldRecommend == goodReview.wouldRecommend &&
          review.review == goodReview.review
        }
      },
      test("Get the review by ids (id, companyId, userId).") {
        for {
          repo               <- ZIO.service[ReviewRepository]
          review             <- repo.create(goodReview)
          fetchedById        <- repo.getById(review.id)
          fetchedByCompanyId <- repo.getByCompanyId(review.companyId)
          fetchedByUserId    <- repo.getByUserId(review.userId)
        } yield assertTrue(
          fetchedById.contains(review) &&
            fetchedByCompanyId.contains(review) &&
            fetchedByUserId.contains(review)
        )
      },
      test("Update the review.") {
        for {
          repo    <- ZIO.service[ReviewRepository]
          review  <- repo.create(goodReview)
          updated <- repo.update(review.id, _.copy(review = "Not too bad."))
        } yield assertTrue(
          review.id == updated.id &&
            updated.companyId == review.companyId &&
            updated.userId == review.userId &&
            updated.management == review.management &&
            updated.culture == review.culture &&
            updated.salary == review.salary &&
            updated.benefits == review.benefits &&
            updated.wouldRecommend == review.wouldRecommend &&
            updated.review == "Not too bad." &&
            updated.created == review.created &&
            updated.updated != review.updated
        )
      },
      test("Delete the review.") {
        for {
          repo        <- ZIO.service[ReviewRepository]
          review      <- repo.create(goodReview)
          _           <- repo.delete(review.id)
          fetchedById <- repo.getById(review.id)
        } yield assertTrue(fetchedById.isEmpty)
      },
      test("Get all reviews.") {
        for {
          repo             <- ZIO.service[ReviewRepository]
          goodReview       <- repo.create(goodReview)
          badReview        <- repo.create(badReview)
          reviewsByCompany <- repo.getByCompanyId(goodReview.companyId)
          reviewsByUser    <- repo.getByUserId(goodReview.userId)
        } yield assertTrue(
          reviewsByCompany.toSet == Set(goodReview, badReview) &&
            reviewsByUser.toSet == Set(goodReview, badReview)
        )
      }
    ).provide(
      ReviewRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
}
