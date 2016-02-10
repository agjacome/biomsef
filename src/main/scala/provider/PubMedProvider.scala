package es.uvigo.ei.sing.biomsef
package provider

import scala.concurrent.Future

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import entity._
import database._
import service.EUtilsService
import util.Page

final class PubMedProvider {

  lazy val eUtils       = new EUtilsService
  lazy val articlesDAO  = new ArticlesDAO
  lazy val authorsDAO   = new AuthorsDAO
  lazy val authoringDAO = new ArticleAuthorsDAO

  def search(query: String, limit: Option[Int], page: Int, pageSize: Int): Future[Page[Article.PMID]] =
    eUtils.searchArticlePMID(query, limit, page, pageSize)

  def download(pmidsToDownload: Set[Article.PMID]): Future[Set[Article]] = {
    val existingArticles = Future.sequence {
      pmidsToDownload.map(articlesDAO.getByPubmedId)
    } map { _.flatten }

    for {
      existing <- existingArticles
      pmids     = pmidsToDownload -- existing.flatMap(_.pubmedId)
      fetched  <- eUtils.fetchPubMedArticles(pmids)
      articles <- insertArticles(fetched)
    } yield existing ++ articles
  }

  private def insertArticles(fetched: Set[(Article, List[Author])]): Future[Set[Article]] =
    Future.sequence(fetched.map { case (article, authors) =>
      for {
        art <- articlesDAO.insert(article)
        aut <- insertOrRetrieveAuthors(authors)
        _   <- associateArticleAuthors(art, aut)
      } yield art
    })

  private def insertOrRetrieveAuthors(as: List[Author]): Future[List[Author]] =
    Future.successful { as.map(insertOrRetrieveAuthor) }

  private def insertOrRetrieveAuthor(a: Author): Author = {
    import scala.concurrent.Await
    import scala.concurrent.duration.Duration

    try {
      Await.result(authorsDAO.getByNameOrInsert(a), Duration.Inf)
    } catch {
      case e: java.sql.SQLIntegrityConstraintViolationException =>
        play.api.Logger.error(s"Error inserting $a, will retry. Cause: ${e.getMessage}")
        Thread.sleep(300L)
        insertOrRetrieveAuthor(a)
    }
  }

  private def associateArticleAuthors(art: Article, auts: List[Author]): Future[Unit] = {
    import scala.concurrent.Await
    import scala.concurrent.duration.Duration
    import authoringDAO.ArticleAuthor

    // FIXME: unsafe .get
    val aas: List[ArticleAuthor] = auts.zipWithIndex map {
      case (a, idx) => (art.id.get, a.id.get, idx + 1)
    }

    // FIXME: this Await stuff is ugly, just fix it already
    Future.successful(aas map {
      a => Await.result(authoringDAO.insert(a), Duration.Inf)
    }).map(_ => ())
  }

}
