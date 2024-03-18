package com.corem

import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

object QuillDemo extends ZIOAppDefault {
  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = ZIO.unit
}

trait JobRepository {
  def create(job: Job): Task[Job]
  def update(id: Long, op: Job => Job): Task[Job]
  def delete(id: Long): Task[Job]
  def getById(id: Long): Task[Option[Job]]
  def get: Task[List[Job]]
}

class JobRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends JobRepository {
  import quill.*
  given schema: SchemaMeta[Job]   = schemaMeta[Job]("jobs")
  given insMeta: InsertMeta[Job]  = insertMeta[Job](_.id)
  given upMeta: UpdateMeta[Job]   = updateMeta[Job](_.id)
  
  def create(job: Job): Task[Job] = ???

  def update(id: Long, op: Job => Job): Task[Job] = ???

  def delete(id: Long): Task[Job] = ???

  def getById(id: Long): Task[Option[Job]] = ???

  def get: Task[List[Job]] = ???
}
