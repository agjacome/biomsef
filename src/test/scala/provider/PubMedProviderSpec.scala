package es.uvigo.ei.sing.biomsef.provider

import play.api.libs.concurrent.Execution.Implicits._
import play.api.test.WithApplication

import es.uvigo.ei.sing.biomsef.BaseSpec
import es.uvigo.ei.sing.biomsef.entity._

class PubMedProviderSpec extends BaseSpec {

  private[this] lazy val pubmed = PubMedProvider()

  "The PubMed Provider" - {

    "can search in PubMed to retrieve a list of PubMedIDs" in {
      whenReady(pubmed search ("antimicrobial peptide", None, 1, 10)) { res =>
        res.totalCount should be >= Size(0)
        res.pageNumber should be (Position(1))
        res.pageSize   should be (Size(10))
        res.list       should have size 10
      }
    }

    "can download articles from PubMed" in new WithApplication {
      val database = DatabaseProfile()

      import database.Documents
      import database.profile.simple._

      val pubmedIds : Set[PubMedId] = Set(11035200L, 21176972L, 18803001L)

      whenReady(pubmed download pubmedIds) { documentIds =>
        database withSession { implicit session =>
          Documents.map(_.id).list       should contain theSameElementsAs documentIds
          Documents.map(_.pubmedId).list should contain theSameElementsAs pubmedIds
        }
      }
    }

  }

}

