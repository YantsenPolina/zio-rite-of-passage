package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.repositories.CompanyRepository
import zio.{Task, ZIO, ZLayer}

trait CompanyService {
  def create(request: CreateCompanyRequest): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
}

class CompanyServiceLive private (repository: CompanyRepository) extends CompanyService {
  override def create(request: CreateCompanyRequest): Task[Company] =
    repository.create(request.toCompany(-1L))

  override def getAll: Task[List[Company]] =
    repository.get

  override def getById(id: Long): Task[Option[Company]] =
    repository.getById(id)

  override def getBySlug(slug: String): Task[Option[Company]] =
    repository.getBySlug(slug)
}

object CompanyServiceLive {
  val layer = ZLayer {
    for {
      repository <- ZIO.service[CompanyRepository]
    } yield new CompanyServiceLive(repository)
  }
}
