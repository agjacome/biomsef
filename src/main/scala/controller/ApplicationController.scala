package es.uvigo.ei.sing.biomsef
package controller

import scala.concurrent.Future

import play.api.Play
import play.api.i18n.Messages.Implicits._
import play.api.libs.json.{ Json, JsValue }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import play.twirl.api.Content

import entity._
import database._

object ApplicationController extends Controller with Authorization {

  import Play.current
  import User.UserForm

  lazy val usersDAO = new UsersDAO

  def index(path: String): Action[AnyContent] =
    Action { Ok(twirl.html.index(httpContext)) }

  def untrail(path: String): Action[AnyContent] =
    Action { MovedPermanently(s"$httpContext$path") }

  def login: Action[JsValue] =
    Action.async(parse.json) {
      request => UserForm.bind(request.body).fold(
        errors => Future.successful(BadRequest(Json.obj("err" -> errors.errorsAsJson))),
        user   => createLoginResponse(user.email, user.pass)
      )
    }

  def logout: Action[AnyContent] =
    Action {
      _.headers.get(authTokenHeader).map(
        token => Ok.discardingToken(token)
      ).getOrElse(BadRequest(Json.obj("err" -> "No token is set")))
    }

  def authPing: Action[AnyContent] =
    AsyncTokenizedAction(parse.anyContent) { token => id => request =>
      usersDAO.get(id) map {
        case Some(_) => Ok(Json.obj("userId" -> id)).addingToken(token -> id)
        case None    => NotFound(Json.obj("err" -> "User not found"))
      }
    }

  private def createLoginResponse(email: String, pass: String): Future[Result] =
    usersDAO.getByEmailAndPass(email, pass) map {
      user => user.map(setToken).getOrElse(NotFound(
        Json.obj("err" -> "Invalid login parameters")
      ))
    }

  private def setToken(user: User): Result = {
    val token = java.util.UUID.randomUUID.toString
    Ok(Json.obj(
      "authToken" -> token,
      "userId"    -> user.id.get
    )).addingToken(token -> user.id.get)
  }

}
