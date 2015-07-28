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

  lazy val searchTermsDAO = new SearchTermsDAO

  def search(query: String, page: Int = 0, pageSize: Int = 50): Future[Page[(Article, Set[Keyword])]] =
    searchKeywords(query).flatMap(ks => searchTermsDAO.searchKeywords(page, pageSize, ks))

  private def searchKeywords(query: String): Future[Set[Keyword.ID]] =
    Future.sequence(searchers.map(_.search(query))).map(_.flatten)

  private def createSearchers(searchers: java.util.List[String]): Set[SearcherAdapter] =
    (searchers.asScala map {
      clazz => Class.forName(clazz).newInstance.asInstanceOf[SearcherAdapter]
    }).toSet

}
