package com.asya.reviewboard.repositories

import com.asya.reviewboard.domain.data.Company
import com.asya.reviewboard.syntax.assert
import zio.{Scope, ZIO, ZLayer}
import zio.test.{Spec, TestEnvironment, ZIOSpecDefault}

import java.sql.SQLException
import javax.sql.DataSource
import scala.util.Random

object CompanyRepositorySpec extends ZIOSpecDefault with RepositorySpec {
  private val rockTheJvmCompany = Company(1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com")

  private def generateCompany(): Company =
    Company(
      id = -1L,
      slug = generateString(),
      name = generateString(),
      url = generateString()
    )

  private def generateString() = Random.alphanumeric.take(8).mkString

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyRepositorySpec")(
      test("Create a company.") {
        val program = for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(rockTheJvmCompany)
        } yield company

        program.assert {
          case Company(_, "rock-the-jvm", "Rock the JVM", "rockthejvm.com", _, _, _, _, _) => true
          case _                                                                           => false
        }
      },
      test("Creating a duplicate company should return error.") {
        val program = for {
          repo  <- ZIO.service[CompanyRepository]
          _     <- repo.create(rockTheJvmCompany)
          error <- repo.create(rockTheJvmCompany).flip
        } yield error

        program.assert(_.isInstanceOf[SQLException])
      },
      test("Get the company by id and slug.") {
        val program = for {
          repo          <- ZIO.service[CompanyRepository]
          company       <- repo.create(rockTheJvmCompany)
          fetchedById   <- repo.getById(company.id)
          fetchedBySlug <- repo.getBySlug(company.slug)
        } yield (company, fetchedById, fetchedBySlug)

        program.assert { case (company, fetchedById, fetchedBySlug) =>
          fetchedById.contains(company) && fetchedBySlug.contains(company)
        }
      },
      test("Update the company.") {
        val program = for {
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(rockTheJvmCompany)
          updated     <- repo.update(company.id, _.copy(url = "blog.rockthejvm.com"))
          fetchedById <- repo.getById(company.id)
        } yield (updated, fetchedById)

        program.assert { case (updated, fetchedById) =>
          fetchedById.contains(updated)
        }
      },
      test("Delete the company.") {
        val program = for {
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(rockTheJvmCompany)
          _           <- repo.delete(company.id)
          fetchedById <- repo.getById(company.id)
        } yield fetchedById

        program.assert(_.isEmpty)
      },
      test("Get all companies.") {
        val program = for {
          repo             <- ZIO.service[CompanyRepository]
          companies        <- ZIO.collectAll((1 to 10).map(_ => repo.create(generateCompany())))
          fetchedCompanies <- repo.get
        } yield (companies, fetchedCompanies)

        program.assert { case (companies, fetchedCompanies) =>
          companies.toSet == fetchedCompanies.toSet
        }
      }
    ).provide(
      CompanyRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
}
