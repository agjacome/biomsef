package es.uvigo.ei.sing.biomsef
package database

import scala.concurrent.Future

import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import slick.driver.JdbcProfile

import entity.User
import util.Page

trait UsersComponent { self: HasDatabaseConfig[JdbcProfile] =>

  import driver.api._

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id    = column[User.ID]("user_id", O.PrimaryKey, O.AutoInc)
    def email = column[String]("user_email")
    def pass  = column[String]("user_password")

    def unique_email = index("idx_users_unique_email", email, unique = true)

    def * = (id.?, email, pass) <> (User.tupled, User.unapply)
  }

  lazy val users = TableQuery[Users]

}

final class UsersDAO extends UsersComponent with HasDatabaseConfig[JdbcProfile] {

  import com.github.t3hnar.bcrypt._

  import driver.api._
  import UsersDAO._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def count: Future[Int] =
    db.run(users.length.result)

  def count(emailFilter: String): Future[Int] =
    db.run(users.filter(_.email.toLowerCase like emailFilter.toLowerCase).length.result)

  def get(id: User.ID): Future[Option[User]] =
    db.run(users.filter(_.id === id).result.headOption)

  def getByEmailAndPass(email: String, pass: String): Future[Option[User]] =
    db.run(users.filter(_.email.toLowerCase === email.toLowerCase).result.headOption) map {
      _.filter(user=> pass.isBcrypted(user.pass))
    }

  def list(page: Int = 0, pageSize: Int = 10, orderBy: OrderBy = OrderByID, emailFilter: String = "%"): Future[Page[User]] = {
    val offset = pageSize * page

    val query = users.filter(
      _.email.toLowerCase like emailFilter.toLowerCase
    ).sortBy(orderBy.order).drop(offset).take(pageSize)

    for {
      total  <- count(emailFilter)
      result <- db.run(query.result)
    } yield Page(result, page, offset, total)
  }

  def insert(user: User): Future[User] = {
    val hashed: User = user.copy(pass = user.pass.bcrypt)
    db.run((users returning users.map(_.id) into ((user, id) => user.copy(id = Some(id)))) += hashed)
  }

  def insert(users: User*): Future[Seq[User]] =  {
    val hashed: Seq[User] = users.map(user => user.copy(pass = user.pass.bcrypt))
    db.run((this.users returning this.users.map(_.id) into ((user, id) => user.copy(id = Some(id)))) ++= hashed)
  }

  def update(id: User.ID, newPass: String): Future[Unit] =
    db.run(users.filter(_.id === id).map(_.pass).update(newPass.bcrypt)).map(_ => ())

  def update(user: User, newPass: String): Future[Unit] =
    user.id.fold(Future.failed[Unit] {
      new IllegalArgumentException("It is impossible to update a user with empty ID")
    })(id => update(id, newPass))

  def delete(id: User.ID): Future[Unit] =
    db.run(users.filter(_.id === id).delete).map(_ => ())

  def delete(user: User): Future[Unit] =
    user.id.fold(Future.failed[Unit] {
      new IllegalArgumentException("It is impossible to delete an user with empty ID")
    })(delete)

}

object UsersDAO {

  import slick.ast.Ordering
  import slick.ast.Ordering.{ Asc, NullsDefault }
  import slick.lifted.ColumnOrdered

  private type Users = UsersComponent#Users

  sealed trait OrderBy {
    type ColumnType
    val order: Users => ColumnOrdered[ColumnType]
  }

  case object OrderByID extends OrderBy {
    type ColumnType = Long
    val order: Users => ColumnOrdered[ColumnType] =
      user => ColumnOrdered(user.id, Ordering(Asc, NullsDefault))
  }

  case object OrderByEmail extends OrderBy {
    type ColumnType = String
    val order: Users => ColumnOrdered[ColumnType] =
      user => ColumnOrdered(user.email, Ordering(Asc, NullsDefault))
  }

}
