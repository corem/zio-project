package com.corem.reviewbord.services

import com.corem.reviewbord.domain.data.*
import com.corem.reviewbord.http.requests.CreateCompanyRequest
import com.corem.reviewbord.repositories.CompanyRepository
import zio.*

trait CompanyService {
  def create(createCompanyRequest: CreateCompanyRequest): Task[Company]
  def getAll: Task[List[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
}

class CompanyServiceLive private (repo: CompanyRepository) extends CompanyService {
  override def create(createCompanyRequest: CreateCompanyRequest): Task[Company] =
    repo.create(createCompanyRequest.toCompany(-1L))
  override def getAll: Task[List[Company]] =
    repo.get
  override def getById(id: Long): Task[Option[Company]] =
    repo.getById(id)
  override def getBySlug(slug: String): Task[Option[Company]] =
    repo.getBySlug(slug)
}

object CompanyServiceLive {
  val layer = ZLayer {
    for {
      repo <- ZIO.service[CompanyRepository]
    } yield new CompanyServiceLive(repo)
  }
}
