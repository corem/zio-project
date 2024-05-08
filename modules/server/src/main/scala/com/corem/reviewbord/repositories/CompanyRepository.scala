package com.corem.reviewbord.repositories

import com.corem.reviewbord.domain.data.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait CompanyRepository {
  def create(company: Company): Task[Company]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def get: Task[List[Company]]
  def update(id: Long, op: Company => Company): Task[Company]
  def delete(id: Long): Task[Company]
}

class CompanyRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends CompanyRepository {
  import quill.*
  inline given schema: SchemaMeta[Company]  = schemaMeta[Company]("companies")
  inline given insMeta: InsertMeta[Company] = insertMeta[Company](_.id)
  inline given upMeta: UpdateMeta[Company]  = updateMeta[Company](_.id)

  override def create(company: Company): Task[Company] =
    run {
      query[Company]
        .insertValue(lift(company))
        .returning(r => r)
    }

  override def getById(id: Long): Task[Option[Company]] =
    run {
      query[Company].filter(_.id == lift(id))
    }.map(_.headOption)

  override def getBySlug(slug: String): Task[Option[Company]] =
    run {
      query[Company].filter(_.slug == lift(slug))
    }.map(_.headOption)

  override def get: Task[List[Company]] =
    run(query[Company])

  override def update(id: Long, op: Company => Company): Task[Company] =
    for {
      current <- getById(id).someOrFail(new RuntimeException(s"Could not update: missing id $id"))
      updated <- run {
        query[Company]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(r => r)
      }
    } yield updated

  override def delete(id: Long): Task[Company] =
    run {
      query[Company]
        .filter(_.id == lift(id))
        .delete
        .returning(r => r)
    }
}

object CompanyRepositoryLive {
  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase.type]].map(quill => CompanyRepositoryLive(quill))
  }
}

object CompanyRepositoryDemo extends ZIOAppDefault {
  val program = for {
    repo <- ZIO.service[CompanyRepository]
    _    <- repo.create(Company(-1L, "corem-corp", "Corem Corp", "corem.com"))
  } yield ()
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.provide(
      CompanyRepositoryLive.layer,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("corem.db")
    )
}
