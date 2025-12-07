package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.repositories.CompanyRepository
import com.rockthejvm.reviewboard.syntax.*
import zio.*
import zio.test.*

object CompanyServiceSpec extends ZIOSpecDefault {
  private val service = ZIO.serviceWithZIO[CompanyService]

  private val repoLayerStub = ZLayer.succeed(
    new CompanyRepository {
      val db = collection.mutable.Map[Long, Company]()

      override def create(company: Company): Task[Company] =
        ZIO.succeed {
          val nextId     = db.keys.maxOption.getOrElse(0L) + 1
          val newCompany = company.copy(id = nextId)
          db += (nextId -> newCompany)
          newCompany
        }

      override def update(id: Long, op: Company => Company): Task[Company] =
        ZIO.attempt {
          val company = db(id)
          db += (id -> op(company))
          company
        }

      override def delete(id: Long): Task[Company] =
        ZIO.attempt {
          val company = db(id)
          db -= id
          company
        }

      override def getById(id: Long): Task[Option[Company]] =
        ZIO.succeed(db.get(id))

      override def getBySlug(slug: String): Task[Option[Company]] =
        ZIO.succeed(db.values.find(_.slug == slug))

      override def get: Task[List[Company]] =
        ZIO.succeed(db.values.toList)
    }
  )

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("CompanyServiceSpec")(
      test("Create a company.") {
        val companyZIO = service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))

        companyZIO.assert { company =>
          company.name == "Rock the JVM" &&
          company.url == "rockthejvm.com" &&
          company.slug == "rock-the-jvm"
        }
      },
      test("Get the company by id.") {
        val program = for {
          company    <- service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
          companyOpt <- service(_.getById(company.id))
        } yield (company, companyOpt)

        program.assert {
          case (company, Some(companyResult)) =>
            company.name == "Rock the JVM" &&
            company.url == "rockthejvm.com" &&
            company.slug == "rock-the-jvm" &&
            company == companyResult
          case _ => false
        }
      },
      test("Get the company by slug.") {
        val program = for {
          company    <- service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
          companyOpt <- service(_.getBySlug(company.slug))
        } yield (company, companyOpt)

        program.assert {
          case (company, Some(companyResult)) =>
            company.name == "Rock the JVM" &&
            company.url == "rockthejvm.com" &&
            company.slug == "rock-the-jvm" &&
            company == companyResult
          case _ => false
        }
      },
      test("Get all companies.") {
        val program = for {
          company1  <- service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
          company2  <- service(_.create(CreateCompanyRequest("Google", "google.com")))
          companies <- service(_.getAll)
        } yield (company1, company2, companies)

        program.assert { case (company1, company2, companies) =>
          companies.toSet == Set(company1, company2)
        }
      }
    ).provide(
      CompanyServiceLive.layer,
      repoLayerStub
    )
}
