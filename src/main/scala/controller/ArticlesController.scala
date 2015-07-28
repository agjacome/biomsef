package es.uvigo.ei.sing.biomsef
package controller

import scala.concurrent.Future

import play.api.Play
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.{ Json, JsValue }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import entity._
import database._
import util.Page

object ArticlesController extends Controller with Authorization {

  import Play.current
  import Article.ArticleForm

  lazy val articlesDAO    = new ArticlesDAO
  lazy val annotationsDAO = new AnnotationsDAO

  def list(page: Option[Int], count: Option[Int]): Action[AnyContent] =
    Action.async {
      articlesDAO.list(page.getOrElse(0), count.getOrElse(50)).map(as => Ok(Json.toJson(as)))
    }

  def get(id: Article.ID): Action[AnyContent] =
    Action.async {
      withAnnotatedArticle(id) { article => Ok(Json.toJson(article)) }
    }

  def add: Action[JsValue] =
    AuthorizedAsyncAction(parse.json) { _ => request =>
      ArticleForm.bind(request.body).fold(
        errors  => Future.successful(BadRequest(Json.obj("err" -> errors.errorsAsJson))),
        article => articlesDAO.insert(article).map(a => Created(Json.toJson(a)))
      )
    }

  def delete(id: Article.ID): Action[AnyContent] =
    AuthorizedAsyncAction(parse.anyContent) { _ => _ =>
      articlesDAO.delete(id).map(_ => NoContent)
    }

  private def withAnnotatedArticle(id: Article.ID)(f: AnnotatedArticle => Result): Future[Result] =
    annotationsDAO.getAnnotatedArticle(id) flatMap {
      case a @ Some(_) => Future.successful(a)
      case None        => articlesDAO.get(id) map { _.map(_.toAnnotatedArticle) }
    } map {
      _.map(f).getOrElse(NotFound(Json.obj("err" -> "Article not found")))
    }

  private implicit class ArticleOps(val a: Article) extends AnyVal {
    def toAnnotatedArticle: AnnotatedArticle = AnnotatedArticle(a, Set.empty, Set.empty)
  }

}
