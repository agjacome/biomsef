package es.uvigo.ei.sing.biomsef
package database

import scala.concurrent.Future

import play.api.Play
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfig }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import slick.driver.JdbcProfile

import entity.Author
import entity.Article
import util.Page

trait ArticleAuthorsComponent {
  self: AuthorsComponent with ArticlesComponent with HasDatabaseConfig[JdbcProfile] =>

  import driver.api._

  type ArticleAuthor = (Article.ID, Author.ID, Int)

  class ArticleAuthors(tag: Tag) extends Table[ArticleAuthor](tag, "article_authors") {
    def articleId = column[Article.ID]("article_id")
    def authorId  = column[Author.ID]("author_id")
    def position  = column[Int]("position")

    def pk = primaryKey("article_authors_primary_key", (articleId, authorId))

    def article = foreignKey("article_authors_article_fk", articleId, articles)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)
    def author  = foreignKey("article_authors_author_fk",  authorId,  authors)(_.id, onUpdate = ForeignKeyAction.Cascade, onDelete = ForeignKeyAction.Cascade)

    def * = (articleId, authorId, position)
  }

  lazy val articleAuthors = TableQuery[ArticleAuthors]

}

final class ArticleAuthorsDAO extends AuthorsComponent with ArticlesComponent with ArticleAuthorsComponent with HasDatabaseConfig[JdbcProfile] {

  import driver.api._
  import ArticleAuthorsDAO._

  protected val dbConfig = DatabaseConfigProvider.get[JdbcProfile](Play.current)

  def get(articleId: Article.ID, authorId: Author.ID): Future[Option[ArticleAuthor]] =
    db.run(articleAuthors.filter(aa =>
      aa.articleId === articleId &&
      aa.authorId  === authorId
    ).result.headOption)

  def getByArticle(articleId: Article.ID): Future[Seq[ArticleAuthor]] =
    db.run(articleAuthors.filter(_.articleId === articleId).result)

  def getByAuthor(authorId: Author.ID): Future[Seq[ArticleAuthor]] =
    db.run(articleAuthors.filter(_.authorId === authorId).result)

  def insert(articleAuthor: ArticleAuthor): Future[Unit] = {
    def insert = (articleAuthors += articleAuthor).map(_ => ())

    def filter = articleAuthors.filter { aa =>
      (aa.articleId === articleAuthor._1) &&
      (aa.authorId  === articleAuthor._2)
    }

    def query = filter.result.headOption flatMap {
      _.fold(insert)(_ => DBIO.successful(()))
    }

    db.run(query)
  }

  def insert(articleAuthors: ArticleAuthor*): Future[Unit] =
    db.run(this.articleAuthors ++= articleAuthors).map(_ => ())

  def delete(articleAuthor: ArticleAuthor): Future[Unit] =
    db.run(articleAuthors.filter(aa =>
      aa.articleId === articleAuthor._1 &&
      aa.authorId  === articleAuthor._2
    ).delete.map(_ => ()))

}

object ArticleAuthorsDAO {

}

