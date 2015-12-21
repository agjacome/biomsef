package es.uvigo.ei.sing.biomsef
package searcher

import scala.collection.JavaConverters._
import scala.concurrent.Future

import play.api.Play
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import entity._
import database._
import util.Page

trait SearcherAdapter {

  lazy val searchTermsDAO = new SearchTermsDAO

  def search(query: String): Future[Set[Keyword.ID]]

  protected final def searchNormalized(terms: Set[String]): Future[Set[Keyword.ID]] = {
    Future.sequence(terms map {
      term => searchTermsDAO.getKeywordIds(s"%$term%")
    }).map(_.flatten)
  }

}

final class Searcher {

  private lazy val searchers = Play.current.configuration.getStringList("searchers")
    .fold(Set.empty[SearcherAdapter])(createSearchers)

  private lazy val simpleSearcherActivated = searchers.exists(_.isInstanceOf[SimpleSearcher])

  lazy val keywordsDAO    = new KeywordsDAO
  lazy val searchTermsDAO = new SearchTermsDAO

  def search(query: String, page: Int = 0, pageSize: Int = 50): Future[Page[(Article, Double, Set[Keyword])]] =
    searchKeywords(query).flatMap(ks => searchTermsDAO.searchKeywords(page, pageSize, ks, 0L, Long.MaxValue))

  def advSearch(query: String, page: Int = 0, pageSize: Int = 50, cats: Set[Category], fromYear: Long, toYear: Long): Future[Page[(Article, Double, Set[Keyword])]] = {
    val ks: Future[Set[Keyword.ID]] = searchKeywords(query).flatMap(
      kids => Future.sequence(kids.map(keywordsDAO.get))
    ).map(_.filter(_.exists(k => cats.apply(k.category))).map(_.get.id.get))

    ks.flatMap(kss => searchTermsDAO.searchKeywords(page, pageSize, kss, fromYear, toYear))
  }

  private def searchKeywords(query: String): Future[Set[Keyword.ID]] = {
    val kf = Future.sequence(
      searchers.filter(!_.isInstanceOf[SimpleSearcher]).map(_.search(query))
    ).map(_.flatten)

    kf flatMap { ks =>
      if (ks.isEmpty && simpleSearcherActivated)
        searchers.filter(_.isInstanceOf[SimpleSearcher]).head.search(query)
      else
        Future.successful(ks)
    }
  }

  private def createSearchers(searchers: java.util.List[String]): Set[SearcherAdapter] =
    (searchers.asScala map {
      clazz => Class.forName(clazz).newInstance.asInstanceOf[SearcherAdapter]
    }).toSet

}
