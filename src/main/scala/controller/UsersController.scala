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

object UsersController extends Controller with Authorization {

  import Play.current
  import User.UserForm

  lazy val usersDAO = new UsersDAO

  // TODO: use of orderBy and emailFilter
  def list(page: Option[Int], count: Option[Int]): Action[AnyContent] =
    AuthorizedAsyncAction(parse.anyContent) { _ => _ =>
      usersDAO.list(page.getOrElse(0), count.getOrElse(50)).map(us => Ok(Json.toJson(us)))
    }

  def get(id: User.ID): Action[AnyContent] =
    AuthorizedAsyncAction(parse.anyContent) { _ => _ => 
      withUser(id) { user => Ok(Json.toJson(user)) }
    }

  def add: Action[JsValue] =
    AuthorizedAsyncAction(parse.json) { _ => request => 
      UserForm.bind(request.body).fold(
        errors => Future.successful(BadRequest(Json.obj("err" -> errors.errorsAsJson))),
        user   => usersDAO.insert(user).map(u => Created(Json.toJson(u)))
      )
    }

  def edit(id: User.ID): Action[JsValue] =
    AuthorizedAsyncAction(parse.json) { _ => request =>
      UserForm.bind(request.body).fold(
        errors => Future.successful(BadRequest(Json.obj("err" -> errors.errorsAsJson))),
        user   => usersDAO.update(id, user.pass).map(_ => NoContent)
      )
    }

  def delete(id: User.ID): Action[AnyContent] =
    AuthorizedAsyncAction(parse.anyContent) { _ => _ =>
      usersDAO.delete(id).map(_ => NoContent)
    }

  private def withUser(id: User.ID)(f: User => Result): Future[Result] =
    usersDAO.get(id) map {
      _.map(f).getOrElse(NotFound(Json.obj("err" -> "User not found")))
    }

}
