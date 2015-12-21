package es.uvigo.ei.sing.biomsef
package database

import scala.concurrent.Future

import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import slick.driver.JdbcProfile

import entity.Author
import util.Page

trait AuthorsComponent { self: HasDatabaseConfig[JdbcProfile] =>

  import driver.api._

  class Authors(tag: Tag) extends Table[Author](tag, "authors") {
    def id        = column[Author.ID]("author_id", O.PrimaryKey, O.AutoInc)
    def lastName  = column[String]("author_last_name")
    def firstName = column[String]("author_first_name")
    def initials  = column[String]("author_initials")

    def * = (id.?, lastName, firstName, initials) <> (Author.tupled, Author.unapply)
  }

  lazy val authors = TableQuery[Authors]

}

final class AuthorsDAO extends AuthorsComponent with HasDatabaseConfig[JdbcProfile] {

  import driver.api._
  import AuthorsDAO._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def count: Future[Int] =
    db.run(authors.length.result)

  def count(filter: Filter): Future[Int] =
    db.run {
      authors.filter(author =>
        (author.firstName.toLowerCase like filter.firstName.toLowerCase) &&
        (author.lastName.toLowerCase  like filter.lastName.toLowerCase) &&
        (author.initials.toLowerCase  like filter.initials.toLowerCase)
      ).length.result
    }

  def get(id: Author.ID): Future[Option[Author]] =
    db.run(authors.filter(_.id === id).result.headOption)

  def getByName(lastName: String, firstName: String, initials: String): Future[Option[Author]] =
    db.run(authors.filter(a ⇒
      a.lastName.toLowerCase  === lastName.toLowerCase  &&
      a.firstName.toLowerCase === firstName.toLowerCase &&
      a.initials.toLowerCase  === initials.toLowerCase
    ).result.headOption)

  def list(page: Int = 0, pageSize: Int = 10, orderBy: OrderBy = OrderByID, filter: Filter = Filter()): Future[Page[Author]] = {
    val offset = pageSize * page

    val query = authors.filter(author =>
      (author.firstName.toLowerCase like filter.firstName.toLowerCase) &&
      (author.lastName.toLowerCase  like filter.lastName.toLowerCase) &&
      (author.initials.toLowerCase  like filter.initials.toLowerCase)
    ).sortBy(orderBy.order).drop(offset).take(pageSize)

    for {
      total  <- count(filter)
      result <- db.run(query.result)
    } yield Page(result, page, offset, total)
  }

  def insert(author: Author): Future[Author] =
    db.run {
      (authors returning authors.map(_.id) into ((author, id) => author.copy(id = Some(id)))) += author
    }

  def insert(authors: Author*): Future[Seq[Author]] =
    db.run {
      (this.authors returning this.authors.map(_.id) into ((author, id) => author.copy(id = Some(id)))) ++= authors
    }

  def update(id: Author.ID, author: Author): Future[Unit] = {
    val updated: Author = author.copy(id = Some(id))
    db.run(authors.filter(_.id === id).update(updated)).map(_ => ())
  }

  def delete(id: Author.ID): Future[Unit] =
    db.run(authors.filter(_.id === id).delete).map(_ => ())

  def delete(author: Author): Future[Unit] =
    author.id.fold(Future.failed[Unit] {
      new IllegalArgumentException("It is impossible to delete an author with empty ID")
    })(delete)

}

object AuthorsDAO {

  import slick.ast.Ordering
  import slick.ast.Ordering.{ Asc, NullsDefault, NullsLast }
  import slick.lifted.ColumnOrdered

  private type Authors = AuthorsComponent#Authors

  final case class Filter (
    firstName: String = "%",
    lastName:  String = "%",
    initials:  String = "%"
  )

  sealed trait OrderBy {
    type ColumnType
    val order: Authors => ColumnOrdered[ColumnType]
  }

  case object OrderByID extends OrderBy {
    type ColumnType = Long
    val order: Authors => ColumnOrdered[ColumnType] =
      author => ColumnOrdered(author.id, Ordering(Asc, NullsDefault))
  }

  case object OrderByFirstName extends OrderBy {
    type ColumnType = String
    val order: Authors => ColumnOrdered[ColumnType] =
      author => ColumnOrdered(author.firstName, Ordering(Asc, NullsDefault))
  }

  case object OrderByLastName extends OrderBy {
    type ColumnType = String
    val order: Authors => ColumnOrdered[ColumnType] =
      author => ColumnOrdered(author.lastName, Ordering(Asc, NullsDefault))
  }

  case object OrderByInitials extends OrderBy {
    type ColumnType = String
    val order: Authors => ColumnOrdered[ColumnType] =
      author => ColumnOrdered(author.initials, Ordering(Asc, NullsDefault))
  }

}
