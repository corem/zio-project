package com.corem.reviewboard.repositories

import com.corem.reviewbord.domain.data.Company
import com.corem.reviewbord.repositories.{CompanyRepository, CompanyRepositoryLive, Repository}
import zio.*
import zio.test.*
import com.corem.reviewboard.syntax.*

import java.sql.SQLException
import javax.sql.DataSource

object CompanyRepositorySpec extends ZIOSpecDefault with RepositorySpec {
  private val myCompany   = Company(1L, "corem-corp", "Corem Corp", "corem.com")
  private def genString() = scala.util.Random.alphanumeric.take(8).mkString
  private def genCompany(): Company = Company(
    id = -1L,
    slug = genString(),
    name = genString(),
    url = genString()
  )

  override val initScript: String = "sql/companies.sql"
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyRepositorySpec")(
      test("Create Company") {
        val program = for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(myCompany)
        } yield company

        program.assert {
          case Company(_, "corem-corp", "Corem Corp", "corem.com", _, _, _, _, _) => true
          case _                                                                  => false
        }
      },
      test("Create a duplicate should raise an error") {
        val program = for {
          repo    <- ZIO.service[CompanyRepository]
          company <- repo.create(myCompany)
          error   <- repo.create(myCompany).flip
        } yield error

        program.assert(_.isInstanceOf[SQLException])
      },
      test("Get by id and slug") {
        val program = for {
          repo          <- ZIO.service[CompanyRepository]
          company       <- repo.create(myCompany)
          fetchedById   <- repo.getById(myCompany.id)
          fetchedBySlug <- repo.getBySlug(myCompany.slug)
        } yield (company, fetchedById, fetchedBySlug)

        program.assert { case (company, fetchedById, fetchedBySlug) =>
          fetchedById.contains(myCompany) && fetchedBySlug.contains(myCompany)
        }
      },
      test("Update Company") {
        val program = for {
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(myCompany)
          updated     <- repo.update(myCompany.id, _.copy(url = "newurl.corem.com"))
          fetchedById <- repo.getById(myCompany.id)
        } yield (updated, fetchedById)

        program.assert { case (updated, fetchedById) =>
          fetchedById.contains(updated)
        }
      },
      test("Delete Company") {
        val program = for {
          repo        <- ZIO.service[CompanyRepository]
          company     <- repo.create(myCompany)
          _           <- repo.delete(myCompany.id)
          fetchedById <- repo.getById(myCompany.id)
        } yield fetchedById

        program.assert(_.isEmpty)
      },
      test("Get All Companies") {
        val program = for {
          repo             <- ZIO.service[CompanyRepository]
          companies        <- ZIO.collectAll((1 to 10).map(_ => repo.create(genCompany())))
          companiesFetched <- repo.get
        } yield (companies, companiesFetched)

        program.assert { case (companies, companiesFetched) =>
          companies.toSet == companiesFetched.toSet
        }
      }
    ).provide(CompanyRepositoryLive.layer, dataSourceLayer, Repository.quillLayer, Scope.default)
}
