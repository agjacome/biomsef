package es.uvigo.ei.sing.biomsef
package database

import scala.concurrent.Future

import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import slick.driver.JdbcProfile

import entity.Article
import util.Page

trait ArticlesComponent { self: HasDatabaseConfig[JdbcProfile] =>

  import driver.api._

  class Articles(tag: Tag) extends Table[Article](tag, "articles") {
    def id           = column[Article.ID]("article_id", O.PrimaryKey, O.AutoInc)
    def pubmedId     = column[Option[Article.PMID]]("article_pmid")
    def title        = column[String]("article_title")
    def content      = column[String]("article_content")
    def isAnnotated  = column[Boolean]("article_is_annotated")
    def isProcessing = column[Boolean]("article_is_processing")

    def unique_pubmedid = index("idx_article_unique_pmid", pubmedId, unique = true)

    def * = (id.?, pubmedId, title, content, isAnnotated, isProcessing) <> (Article.tupled, Article.unapply)
  }

  lazy val articles = TableQuery[Articles]

}

final class ArticlesDAO extends ArticlesComponent with HasDatabaseConfig[JdbcProfile] {

  import driver.api._
  import ArticlesDAO._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def count: Future[Int] =
    db.run(articles.length.result)

  def count(filter: Filter): Future[Int] =
    db.run {
      val f1 = articles.filter(_.title.toLowerCase like filter.title.toLowerCase)
      val f2 = filter.isAnnotated.fold(f1)(b => f1.filter(_.isAnnotated === b))
      val f3 = filter.isProcessing.fold(f2)(b => f2.filter(_.isProcessing === b))

      f3.length.result
    }

  def get(id: Article.ID): Future[Option[Article]] =
    db.run(articles.filter(_.id === id).result.headOption)

  def getByPubmedId(pubmedId: Long): Future[Option[Article]] =
    db.run(articles.filter(_.pubmedId === pubmedId).result.headOption.transactionally)

  def list(page: Int = 0, pageSize: Int = 10, orderBy: OrderBy = OrderByID, filter: Filter = Filter()): Future[Page[Article]] = {
    val offset = pageSize * page

    val query = {
      val f1 = articles.filter(_.title.toLowerCase like filter.title.toLowerCase)
      val f2 = filter.isAnnotated.fold(f1)(b => f1.filter(_.isAnnotated === b))
      val f3 = filter.isProcessing.fold(f2)(b => f2.filter(_.isProcessing === b))

      f3.sortBy(orderBy.order).drop(offset).take(pageSize)
    }

    for {
      total  <- count(filter)
      result <- db.run(query.result)
    } yield Page(result, page, offset, total)
  }

  def insert(article: Article): Future[Article] =
    db.run {
      ((articles returning articles.map(_.id) into ((article, id) => article.copy(id = Some(id)))) += article).transactionally
    }

  def insert(articles: Article*): Future[Seq[Article]] =
    db.run {
      ((this.articles returning this.articles.map(_.id) into ((article, id) => article.copy(id = Some(id)))) ++= articles).transactionally
    }

  def update(id: Article.ID, article: Article): Future[Unit] = {
    val updated: Article = article.copy(id = Some(id))
    db.run(articles.filter(_.id === id).update(updated).transactionally).map(_ => ())
  }

  def update(article: Article, newIsAnnotated: Boolean, newIsProcessing: Boolean): Future[Unit] =
    article.id.fold(Future.failed[Unit] {
      new IllegalArgumentException("It is impossible to update an article with empty ID")
    })(id => update(id, article.copy(isAnnotated = newIsAnnotated, isProcessing = newIsProcessing)))

  def delete(id: Article.ID): Future[Unit] =
    db.run(articles.filter(_.id === id).delete.transactionally).map(_ => ())

  def delete(article: Article): Future[Unit] =
    article.id.fold(Future.failed[Unit] {
      new IllegalArgumentException("It is impossible to delete an article with empty ID")
    })(delete)

}

object ArticlesDAO {

  import slick.ast.Ordering
  import slick.ast.Ordering.{ Asc, NullsDefault, NullsLast }
  import slick.lifted.ColumnOrdered

  private type Articles = ArticlesComponent#Articles

  final case class Filter (
    title:        String          = "%",
    isAnnotated:  Option[Boolean] = None,
    isProcessing: Option[Boolean] = None
  )

  sealed trait OrderBy {
    type ColumnType
    val order: Articles => ColumnOrdered[ColumnType]
  }

  case object OrderByID extends OrderBy {
    type ColumnType = Long
    val order: Articles => ColumnOrdered[ColumnType] =
      article => ColumnOrdered(article.id, Ordering(Asc, NullsDefault))
  }

  case object OrderByPMID extends OrderBy {
    type ColumnType = Option[Long]
    val order: Articles => ColumnOrdered[ColumnType] =
      article => ColumnOrdered(article.pubmedId, Ordering(Asc, NullsLast))
  }

  case object OrderByTitle extends OrderBy {
    type ColumnType = String
    val order: Articles => ColumnOrdered[ColumnType] =
      article => ColumnOrdered(article.title, Ordering(Asc, NullsDefault))
  }

  case object OrderByAnnotated extends OrderBy {
    type ColumnType = Boolean
    val order: Articles => ColumnOrdered[ColumnType] =
      article => ColumnOrdered(article.isAnnotated, Ordering(Asc, NullsDefault))
  }

  case object OrderByProcessing extends OrderBy {
    type ColumnType = Boolean
    val order: Articles => ColumnOrdered[ColumnType] =
      article => ColumnOrdered(article.isProcessing, Ordering(Asc, NullsDefault))
  }

}
