package com.corem.reviewboard.services

import com.corem.reviewbord.http.requests.CreateCompanyRequest
import com.corem.reviewbord.services.{CompanyService, CompanyServiceLive}
import com.corem.reviewboard.syntax.*
import com.corem.reviewbord.domain.data.Company
import com.corem.reviewbord.repositories.CompanyRepository
import zio.*
import zio.test.*

object CompanyServiceSpec extends ZIOSpecDefault {

  val service = ZIO.serviceWithZIO[CompanyService]

  val stubRepoLayer = ZLayer.succeed(
    new CompanyRepository {
      val db = collection.mutable.Map[Long, Company]()

      override def create(company: Company): Task[Company] = ZIO.succeed {
        val nextId     = db.keys.maxOption.getOrElse(0L) + 1
        val newCompany = company.copy(id = nextId)
        db += (nextId -> newCompany)
        newCompany
      }

      override def getById(id: Long): Task[Option[Company]] =
        ZIO.succeed {
          db.get(id)
        }

      override def getBySlug(slug: String): Task[Option[Company]] =
        ZIO.succeed {
          db.values.find(_.slug == slug)
        }

      override def get: Task[List[Company]] =
        ZIO.succeed {
          db.values.toList
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
    }
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("CompanyServiceSpec")(
      test("Create") {
        val companyZIO = service(_.create(CreateCompanyRequest("Corem Corp", "corem.com")))

        companyZIO.assert { company =>
          company.name == "Corem Corp" &&
          company.url == "corem.com" &&
          company.slug == "corem-corp"
        }
      },
      test("Get by Id") {
        val program = for {
          company    <- service(_.create(CreateCompanyRequest("Corem Corp", "corem.com")))
          companyOpt <- service(_.getById(company.id))
        } yield (company, companyOpt)

        program.assert {
          case (company, Some(companyRes)) =>
            company.name == "Corem Corp" &&
            company.url == "corem.com" &&
            company.slug == "corem-corp" &&
            company == companyRes
          case _ => false
        }
      },
      test("Get by Slug") {
        val program = for {
          company    <- service(_.create(CreateCompanyRequest("Corem Corp", "corem.com")))
          companyOpt <- service(_.getBySlug(company.slug))
        } yield (company, companyOpt)

        program.assert {
          case (company, Some(companyRes)) =>
            company.name == "Corem Corp" &&
            company.url == "corem.com" &&
            company.slug == "corem-corp" &&
            company == companyRes
          case _ => false
        }
      },
      test("Get All") {
        val program = for {
          company1  <- service(_.create(CreateCompanyRequest("Corem Corp", "corem.com")))
          company2  <- service(_.create(CreateCompanyRequest("Corem Inc", "corem.inc")))
          companies <- service(_.getAll)
        } yield (company1, company2, companies)

        program.assert { case (company1, company2, companies) =>
          companies.toSet == Set(company1, company2)
        }
      }
    ).provide(CompanyServiceLive.layer, stubRepoLayer)
}
