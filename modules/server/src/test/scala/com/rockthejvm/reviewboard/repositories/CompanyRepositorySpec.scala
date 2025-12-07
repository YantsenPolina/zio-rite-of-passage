package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.syntax.*
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.PostgreSQLContainer
import zio.*
import zio.test.*

import java.sql.SQLException
import javax.sql.DataSource

object CompanyRepositorySpec extends ZIOSpecDefault with RepositorySpec {
  private val rockTheJvmCompany = Company(1L, "rock-th-jvm", "Rock the JVM", "rockthejvm.com")

  private def generateString() =
    scala.util.Random.alphanumeric.take(8).mkString

  private def generateCompany =
    Company(
      id = -1L,
      slug = generateString(),
      name = generateString(),
      url = generateString()
    )

  override def spec: Spec[TestEnvironment & Scope, Any] = {
    suite("CompanyRepositorySpec")(
      test("Create a company.") {
        val program = for {
          repository <- ZIO.service[CompanyRepository]
          company    <- repository.create(rockTheJvmCompany)
        } yield company

        program.assert {
          case Company(_, "rock-th-jvm", "Rock the JVM", "rockthejvm.com", _, _, _, _, _) => true
          case _                                                                          => false
        }
      },
      test("Create a duplicate company should return error.") {
        val program = for {
          repository <- ZIO.service[CompanyRepository]
          company    <- repository.create(rockTheJvmCompany)
          error      <- repository.create(rockTheJvmCompany).flip
        } yield error

        program.assert(_.isInstanceOf[SQLException])
      },
      test("Get the company by id and by slug.") {
        val program = for {
          repository    <- ZIO.service[CompanyRepository]
          company       <- repository.create(rockTheJvmCompany)
          fetchedById   <- repository.getById(company.id)
          fetchedBySlug <- repository.getBySlug(company.slug)
        } yield (company, fetchedById, fetchedBySlug)

        program.assert { case (company, fetchedById, fetchedBySlug) =>
          fetchedById.contains(company) && fetchedBySlug.contains(company)
        }
      },
      test("Update the company.") {
        val program = for {
          repository  <- ZIO.service[CompanyRepository]
          company     <- repository.create(rockTheJvmCompany)
          updated     <- repository.update(company.id, _.copy(url = "blog.rockthejvm.com"))
          fetchedById <- repository.getById(company.id)
        } yield (updated, fetchedById)

        program.assert { case (updated, fetchedById) =>
          fetchedById.contains(updated)
        }
      },
      test("Delete the company.") {
        val program = for {
          repository  <- ZIO.service[CompanyRepository]
          company     <- repository.create(rockTheJvmCompany)
          _           <- repository.delete(company.id)
          fetchedById <- repository.getById(company.id)
        } yield fetchedById

        program.assert(_.isEmpty)
      },
      test("Get companies.") {
        val program = for {
          repository       <- ZIO.service[CompanyRepository]
          companies        <- ZIO.collectAll((1 to 10).map(_ => repository.create(generateCompany)))
          companiesFetched <- repository.get
        } yield (companies, companiesFetched)

        program.assert { case (companies, companiesFetched) =>
          companies.toSet == companiesFetched.toSet
        }
      }
    ).provide(
      CompanyRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )
  }
}
