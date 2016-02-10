package es.uvigo.ei.sing.biomsef
package database

import scala.concurrent.Future

import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import slick.driver.JdbcProfile

import entity._
import util.Page

trait SearchTermsComponent {
  self: ArticlesComponent with KeywordsComponent with HasDatabaseConfig[JdbcProfile] =>

  import driver.api._

  class SearchTerms(tag: Tag) extends Table[SearchTerm](tag, "search_index") {
    def term         = column[String]("search_index_term")
    def tf           = column[Double]("search_index_tf")
    def idf          = column[Double]("search_index_idf")
    def tfidf        = column[Double]("search_index_tfidf")
    def articleId    = column[Article.ID]("article_id")
    def keywordId    = column[Keyword.ID]("keyword_id")

    def pk = primaryKey("search_index_pk", (term, keywordId, articleId))

    def keyword    = foreignKey("search_index_keyword_fk", keywordId, keywords)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
    def article    = foreignKey("search_index_article_fk", articleId, articles)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

    def * = (term, tf, idf, tfidf, articleId, keywordId) <> (SearchTerm.tupled, SearchTerm.unapply)
  }

  lazy val terms = TableQuery[SearchTerms]

}

final class SearchTermsDAO extends SearchTermsComponent with ArticlesComponent with KeywordsComponent with HasDatabaseConfig[JdbcProfile] {

  import driver.api._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def count: Future[Int] =
    db.run(terms.length.result)

  def count(termFilter: String): Future[Int] =
    db.run {
      terms.filter(_.term.toLowerCase like termFilter.toLowerCase).length.result
    }

  def get(id: SearchTerm.ID): Future[Option[SearchTerm]] =
    db.run {
      terms.filter(term =>
        (term.articleId        === id._2) &&
        (term.keywordId        === id._3) &&
        (term.term.toLowerCase === id._1.toLowerCase)
      ).result.headOption
    }

  def getKeywordIds(termFilter: String = "%"): Future[Set[Keyword.ID]] =
    db.run(terms.filter(
      _.term.toLowerCase like termFilter.toLowerCase
    ).groupBy(_.keywordId).map(_._1).result).map(_.toSet)

  def searchTerm(page: Int = 0, pageSize: Int = 10, termFilter: String = "%"): Future[Page[SearchTerm]] = {
    val offset = pageSize * page

    val query = terms.filter(
      _.term.toLowerCase like termFilter.toLowerCase
    ).sortBy(_.tfidf.desc).drop(offset).take(pageSize)

    for {
      total  <- count(termFilter)
      result <- db.run(query.result)
    } yield Page(result, page, offset, total)
  }

  // TODO: uglyness at its finest, clean up
  def searchKeywords(page: Int = 0, pageSize: Int = 10, keywordIds: Set[Keyword.ID], fromYear: Long, toYear: Long): Future[Page[(Article, Double, Set[Keyword])]] = {
    val offset = pageSize * page

    val total = (for {
      term    <- terms         if term.keywordId inSet keywordIds
      article <- this.articles if article.id === term.articleId && article.year >= fromYear && article.year <= toYear
    } yield term).groupBy(_.articleId).map(_._1).length

    val articleIds = (for {
      term    <- terms         if term.keywordId inSet keywordIds
      article <- this.articles if article.id === term.articleId && article.year >= fromYear && article.year <= toYear
    } yield term)
      .groupBy { _.articleId }
      .map     { case (id, ts) => id -> ts.map(_.tfidf).sum }
      .sortBy  { case (_, sum) => sum.desc }
      .drop    { offset   }
      .take    { pageSize }

    val articles = articleIds flatMap { case (id, sum) =>
      this.articles
        .filter { _.id === id }
        .map    { (_, sum)    }
    }

    val found = for {
      term    <- terms
      article <- articles if article._1.id === term.articleId // && article._1.year >= fromYear && article._1.year <= toYear
      keyword <- keywords if keyword.id    === term.keywordId
    } yield (article, keyword)

    val query = found.result map {
      _.groupBy(_._1).mapValues(_.map(_._2).toSet).toList
    }

    for {
      total  <- db.run(total.result)
      result <- db.run(query)
    } yield Page(
      result map { res => (res._1._1, res._1._2.get, res._2) } sortWith { _._2 > _._2 },
      page, offset, total
    )

  }

  def insert(term: SearchTerm): Future[SearchTerm] =
    db.run((terms += term)).map(_ => term)

  def insert(terms: SearchTerm*): Future[Seq[SearchTerm]] =
    db.run((this.terms ++= terms)).map(_ => terms)

  def clear(): Future[Unit] =
    db.run(terms.delete.transactionally).map(_ => ())

}
