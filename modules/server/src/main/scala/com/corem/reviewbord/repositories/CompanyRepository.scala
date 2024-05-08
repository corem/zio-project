package com.corem.reviewbord.repositories

import com.corem.reviewbord.domain.data.*
import zio.*

trait CompanyRepository {
  def create(company: Company): Task[Company]
  def update(id: Long, op: Company => Company): Task[Company]
  def delete(id: Long): Task[Option[Company]]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def get: Task[List[Company]]
}

class CompanyRepositoryLive()
