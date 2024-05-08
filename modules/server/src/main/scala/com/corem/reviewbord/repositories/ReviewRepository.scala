package com.corem.reviewbord.repositories

import com.corem.reviewbord.domain.data.Review
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait ReviewRepository {
  def create(review: Review): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(id: Long): Task[List[Review]]
  def getByUserId(id: Long): Task[List[Review]]
  def update(id: Long, op: Review => Review): Task[Review]
  def delete(id: Long): Task[Review]
}

class ReviewRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends ReviewRepository {
  import quill.*

  inline given schema: SchemaMeta[Review]  = schemaMeta[Review]("reviews")
  inline given insMeta: InsertMeta[Review] = insertMeta[Review](_.id, _.created, _.updated)
  inline given upMeta: UpdateMeta[Review] =
    updateMeta[Review](_.id, _.companyId, _.userId, _.created)

  override def create(review: Review): Task[Review] =
    run {
      query[Review]
        .insertValue(lift(review))
        .returning(r => r)
    }
  override def getById(id: Long): Task[Option[Review]] =
    run {
      query[Review]
        .filter(_.id == lift(id))
    }.map(_.headOption)

  override def getByCompanyId(id: Long): Task[List[Review]] =
    run {
      query[Review]
        .filter(_.companyId == lift(id))
    }
  override def getByUserId(id: Long): Task[List[Review]] =
    run {
      query[Review]
        .filter(_.userId == lift(id))
    }
  override def update(id: Long, op: Review => Review): Task[Review] =
    for {
      current <- getById(id).someOrFail(new RuntimeException(s"Could not update: missing id $id"))
      updated <- run {
        query[Review]
          .filter(_.id == lift(id))
          .updateValue(lift(op(current)))
          .returning(r => r)
      }
    } yield updated
  override def delete(id: Long): Task[Review] =
    run {
      query[Review]
        .filter(_.id == lift(id))
        .delete
        .returning(r => r)
    }
}

object ReviewRepositoryLive {
  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase.type]].map(quill => ReviewRepositoryLive(quill))
  }
}
