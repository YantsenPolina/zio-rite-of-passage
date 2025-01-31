package com.asya.reviewboard.services

import com.asya.reviewboard.domain.data.Company
import com.asya.reviewboard.http.requests.CreateCompanyRequest
import com.asya.reviewboard.repositories.CompanyRepository
import zio.{Task, URLayer, ZIO, ZLayer}

trait CompanyService {
  def create(request: CreateCompanyRequest): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
}

//object CompanyService {
//  val dummyLayer: ULayer[CompanyServiceDummy] = ZLayer.succeed(new CompanyServiceDummy)
//}

class CompanyServiceLive private (repo: CompanyRepository) extends CompanyService {
  override def create(request: CreateCompanyRequest): Task[Company] =
    repo.create(request.toCompany(-1L))

  override def getAll: Task[List[Company]] = repo.get

  override def getById(id: Long): Task[Option[Company]] = repo.getById(id)

  override def getBySlug(slug: String): Task[Option[Company]] = repo.getBySlug(slug)
}

object CompanyServiceLive {
  val layer: URLayer[CompanyRepository, CompanyServiceLive] = ZLayer {
    for {
      repo <- ZIO.service[CompanyRepository]
    } yield new CompanyServiceLive(repo)
  }
}

//class CompanyServiceDummy extends CompanyService {
//  private val db = mutable.Map[Long, Company]()
//
//  override def create(request: CreateCompanyRequest): Task[Company] =
//    ZIO.succeed {
//      val newId      = db.keys.maxOption.getOrElse(0L) + 1
//      val newCompany = request.toCompany(newId)
//      db += (newId -> newCompany)
//      newCompany
//    }
//
//  override def getAll: Task[List[Company]] =
//    ZIO.succeed(db.values.toList)
//
//  override def getById(id: Long): Task[Option[Company]] =
//    ZIO.succeed(db.get(id))
//
//  override def getBySlug(slug: String): Task[Option[Company]] =
//    ZIO.succeed(db.values.find(_.slug == slug))
//}
