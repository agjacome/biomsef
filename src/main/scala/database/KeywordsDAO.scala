package es.uvigo.ei.sing.biomsef
package database

import scala.concurrent.Future

import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import slick.driver.JdbcProfile

import entity.{ Category, Keyword }
import util.Page

trait KeywordsComponent { self: HasDatabaseConfig[JdbcProfile] =>

  import driver.api._

  implicit def CategoryColumnType: BaseColumnType[Category] =
    MappedColumnType.base[Category, Long](_.id, Category.fromId)

  class Keywords(tag: Tag) extends Table[Keyword](tag, "keywords") {
    def id          = column[Keyword.ID]("keyword_id", O.PrimaryKey, O.AutoInc)
    def normalized  = column[String]("keyword_normalized")
    def category    = column[Category]("keyword_category")

    def unique_normalized_category = index("idx_keywords_unique_normalized_category", (normalized, category), unique = true)

    def * = (id.?, normalized, category) <> (Keyword.tupled, Keyword.unapply)
  }

  lazy val keywords = TableQuery[Keywords]

}

final class KeywordsDAO extends KeywordsComponent with HasDatabaseConfig[JdbcProfile] {

  import driver.api._
  import KeywordsDAO._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def count: Future[Int] =
    db.run(keywords.length.result)

  def count(normalizedFilter: String): Future[Int] =
    db.run(keywords.filter(_.normalized.toLowerCase like normalizedFilter.toLowerCase).length.result)

  def get(id: Keyword.ID): Future[Option[Keyword]] =
    db.run(keywords.filter(_.id === id).result.headOption)

  def getByNormalized(normalized: String): Future[Option[Keyword]] =
    db.run(keywords.filter(_.normalized.toLowerCase === normalized.toLowerCase).result.headOption.transactionally)

  def list(page: Int = 0, pageSize: Int = 10, orderBy: OrderBy = OrderByID, normalizedFilter: String = "%"): Future[Page[Keyword]] = {
    val offset = pageSize * page

    val query = keywords.filter(
      _.normalized.toLowerCase like normalizedFilter.toLowerCase
    ).sortBy(orderBy.order).drop(offset).take(pageSize)

    for {
      total  <- count(normalizedFilter)
      result <- db.run(query.result)
    } yield Page(result, page, offset, total)
  }

  def getOrInsert(norm: String, cat: Category, keyword: => Keyword): Future[Keyword] = {
    def insert: DBIOAction[Keyword, NoStream, Effect.Write] =
      (keywords returning keywords.map(_.id) into ((keyword, id) => keyword.copy(id = Some(id)))) += keyword

    val filter = keywords filter { k =>
      k.normalized.toLowerCase === norm.toLowerCase &&
      k.category               === cat
    }

    val query = filter.result.headOption flatMap {
      _.fold(insert)(DBIO.successful)
    }

    db.run(query.transactionally)
  }

  def insert(keyword: Keyword): Future[Keyword] =
    db.run {
      ((keywords returning keywords.map(_.id) into ((keyword, id) => keyword.copy(id = Some(id)))) += keyword).transactionally
    }

  def insert(keywords: Keyword*): Future[Seq[Keyword]] =
    db.run {
      ((this.keywords returning this.keywords.map(_.id) into ((keyword, id) => keyword.copy(id = Some(id)))) ++= keywords).transactionally
    }

  def delete(id: Keyword.ID): Future[Unit] =
    db.run(keywords.filter(_.id === id).delete.transactionally).map(_ => ())

  def delete(keyword: Keyword): Future[Unit] =
    keyword.id.fold(Future.failed[Unit] {
      new IllegalArgumentException("It is impossible to delete a keyword with empty ID")
    })(delete)

}

object KeywordsDAO {

  import slick.ast.Ordering
  import slick.ast.Ordering.{ Asc, NullsDefault }
  import slick.lifted.ColumnOrdered

  private type Keywords = KeywordsComponent#Keywords

  sealed trait OrderBy {
     type ColumnType
     val order: Keywords => ColumnOrdered[ColumnType]
  }

  case object OrderByID extends OrderBy {
    type ColumnType = Long
    val order: Keywords => ColumnOrdered[ColumnType] =
      keyword => ColumnOrdered(keyword.id, Ordering(Asc, NullsDefault))
  }

  case object OrderByNormalized extends OrderBy {
    type ColumnType = String
    val order: Keywords => ColumnOrdered[ColumnType] =
      keyword => ColumnOrdered(keyword.normalized, Ordering(Asc, NullsDefault))
  }

  case object OrderByCategory extends OrderBy {
    type ColumnType = Category
    val order: Keywords => ColumnOrdered[ColumnType] =
      keyword => ColumnOrdered(keyword.category, Ordering(Asc, NullsDefault))
  }

}
