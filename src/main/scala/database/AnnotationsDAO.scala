package es.uvigo.ei.sing.biomsef
package database

import scala.concurrent.Future

import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import slick.driver.JdbcProfile

import entity._
import util.Page

trait AnnotationsComponent {
  self: ArticlesComponent with KeywordsComponent with AuthorsComponent with ArticleAuthorsComponent with HasDatabaseConfig[JdbcProfile] =>

  import driver.api._

  class Annotations(tag: Tag) extends Table[Annotation](tag, "annotations") {
    def id        = column[Annotation.ID]("annotation_id", O.PrimaryKey, O.AutoInc)
    def articleId = column[Article.ID]("article_id")
    def keywordId = column[Keyword.ID]("keyword_id")
    def text      = column[String]("annotation_text")
    def start     = column[Long]("annotation_start")
    def end       = column[Long]("annotation_end")

    def article = foreignKey("annotation_article_fk", articleId, articles)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
    def keyword = foreignKey("annotation_keyword_fk", keywordId, keywords)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

    def * = (id.?, articleId, keywordId, text, start, end) <> (Annotation.tupled, Annotation.unapply)
  }

  lazy val annotations = TableQuery[Annotations]

}

final class AnnotationsDAO extends AnnotationsComponent with ArticlesComponent with KeywordsComponent with AuthorsComponent with ArticleAuthorsComponent with HasDatabaseConfig[JdbcProfile] {

  import driver.api._
  import AnnotationsDAO._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def count: Future[Int] =
    db.run(annotations.length.result)

  def count(filter: Filter): Future[Int] =
    db.run {
      val f1 = annotations.filter(_.text.toLowerCase like filter.text.toLowerCase)
      val f2 = filter.articleId.fold(f1)(id => f1.filter(_.articleId === id))
      val f3 = filter.keywordId.fold(f2)(id => f2.filter(_.keywordId === id))

      f3.length.result
    }

  def get(id: Annotation.ID): Future[Option[Annotation]] =
    db.run(annotations.filter(_.id === id).result.headOption)

  def getAnnotatedArticle(id: Article.ID): Future[Option[AnnotatedArticle]] = {
    val joined = for {
      annotation <- annotations
      keyword    <- keywords if annotation.keywordId === keyword.id
      article    <- articles if annotation.articleId === article.id
      if article.id === id
    } yield (article, keyword, annotation)

    val articleAuthors = for {
      authoring <- this.articleAuthors
      author    <- authors if authoring.authorId === author.id && authoring.articleId === id
    } yield (author, authoring.position)

    val query       = joined.sortBy(_._1.id)
    val authorQuery = articleAuthors.sortBy(_._2).map(_._1)

    val executed = for {
      tuples  <- db.run(query.result)
      authors <- db.run(authorQuery.result)
    } yield (tuples, authors)

    executed map { case (tuples, authors) =>
      val keywords    = tuples.map(_._2).toSet
      val annotations = tuples.map(_._3).toSet
      val article     = tuples.headOption.map(_._1)

      article.map(a => AnnotatedArticle(a, authors.toList, annotations, keywords))
    }
  }

  def countByKeyword: Future[Map[Keyword.ID, Int]] =
    db.run(annotations.groupBy(_.keywordId) map {
      case (kid, as) => (kid -> as.length)
    } result).map(_.toMap)

  def countByArticle: Future[Map[Article.ID, Int]] =
    db.run(annotations.groupBy(_.articleId) map {
      case (aid, as) => (aid -> as.length)
    } result).map(_.toMap)

  def countByArticleAndKeyword: Future[Map[(Article.ID, Keyword.ID), Int]] =
    db.run(annotations.groupBy(a => (a.articleId, a.keywordId)) map {
      case ((aid, kid), as) => ((aid, kid) -> as.length)
    } result).map(_.toMap)

  def list(page: Int = 0, pageSize: Int = 10, orderBy: OrderBy = OrderByID, filter: Filter = Filter()): Future[Page[Annotation]] = {
    val offset = pageSize * page

    val query = {
      val f1 = annotations.filter(_.text.toLowerCase like filter.text.toLowerCase)
      val f2 = filter.articleId.fold(f1)(id => f1.filter(_.articleId === id))
      val f3 = filter.keywordId.fold(f2)(id => f2.filter(_.keywordId === id))

      f3.sortBy(orderBy.order).drop(offset).take(pageSize)
    }

    for {
      total  <- count(filter)
      result <- db.run(query.result)
    } yield Page(result, page, offset, total)
  }

  def insert(annotation: Annotation): Future[Annotation] =
    db.run {
      (annotations returning annotations.map(_.id) into ((annotation, id) => annotation.copy(id = Some(id)))) += annotation
    }

  def insert(annotations: Annotation*): Future[Seq[Annotation]] =
    db.run {
      (this.annotations returning this.annotations.map(_.id) into ((annotation, id) => annotation.copy(id = Some(id)))) ++= annotations
    }

  def update(id: Annotation.ID, annotation: Annotation): Future[Unit] = {
    val updated: Annotation = annotation.copy(id = Some(id))
    db.run(annotations.filter(_.id === id).update(updated)).map(_ => ())
  }

  def update(annotation: Annotation): Future[Unit] =
    annotation.id.fold(Future.failed[Unit] {
      new IllegalArgumentException("It is impossible to update an annotation with empty ID")
    })(id => update(id, annotation))

  def delete(id: Annotation.ID): Future[Unit] =
    db.run(annotations.filter(_.id === id).delete).map(_ => ())

  def delete(annotation: Annotation): Future[Unit] =
    annotation.id.fold(Future.failed[Unit] {
      new IllegalArgumentException("It is impossible to delete an annotation with empty ID")
    })(delete)

  def deleteAnnotationsOf(id: Article.ID): Future[Unit] =
    db.run(annotations.filter(_.articleId === id).delete).map(_ => ())

}

object AnnotationsDAO {

  import slick.ast.Ordering
  import slick.ast.Ordering.{ Asc, NullsDefault }
  import slick.lifted.ColumnOrdered

  private type Annotations = AnnotationsComponent#Annotations

  final case class Filter (
    text:      String             = "%",
    articleId: Option[Article.ID] = None,
    keywordId: Option[Keyword.ID] = None
  )

  sealed trait OrderBy {
    type ColumnType
    val order: Annotations => ColumnOrdered[ColumnType]
  }

  case object OrderByID extends OrderBy {
    type ColumnType = Long
    val order: Annotations => ColumnOrdered[ColumnType] =
      annotation => ColumnOrdered(annotation.id, Ordering(Asc, NullsDefault))
  }

  case object OrderByArticleID extends OrderBy {
    type ColumnType = Long
    val order: Annotations => ColumnOrdered[ColumnType] =
      annotation => ColumnOrdered(annotation.articleId, Ordering(Asc, NullsDefault))
  }

  case object OrderByKeywordID extends OrderBy {
    type ColumnType = Long
    val order: Annotations => ColumnOrdered[ColumnType] =
      annotation => ColumnOrdered(annotation.keywordId, Ordering(Asc, NullsDefault))
  }

  case object OrderByText extends OrderBy {
    type ColumnType = String
    val order: Annotations => ColumnOrdered[ColumnType] =
      annotation => ColumnOrdered(annotation.text, Ordering(Asc, NullsDefault))
  }

}
