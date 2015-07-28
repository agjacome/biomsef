package es.uvigo.ei.sing.biomsef
package controller

import scala.concurrent.duration._
import scala.concurrent.Future

import play.api.Play
import play.api.cache.Cache
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._

import entity._

trait Authorization { self: Controller =>

  import Play.current

  type Token = String

  lazy val authTokenHeader    = "X-XSRF-TOKEN"
  lazy val authTokenCookieKey = "XSRF-TOKEN"
  lazy val authTokenURLKey    = "auth"

  lazy val sessionTimeout = current.configuration.getMilliseconds("sessionTimeout").getOrElse(600000L).milliseconds

  implicit final class ResultWithToken(result: Result) {

    def addingToken(kv: (Token, User.ID)): Result = {
      Cache.set(kv._1, kv._2, sessionTimeout.toSeconds.toInt)
      result.withCookies(Cookie(authTokenCookieKey, kv._1, None, httpOnly = false))
    }

    def discardingToken(token: Token): Result = {
      Cache.remove(token)
      result.discardingCookies(DiscardingCookie(name = authTokenCookieKey))
    }

  }

  def TokenizedAction[A](p: BodyParser[A] = parse.anyContent)(f: Token => User.ID => Request[A] => Result): Action[A] =
    Action(p) { request => withToken(request)(f) }

  def AsyncTokenizedAction[A](p: BodyParser[A] = parse.anyContent)(f: Token => User.ID => Request[A] => Future[Result]): Action[A] =
    Action.async(p) { request => withAsyncToken(request)(f) }

  def AuthorizedAction[A](p: BodyParser[A] = parse.anyContent)(f: User.ID => Request[A] => Result): Action[A] =
    Action(p) { request => withToken(request)(_ => f) }

  def AuthorizedAsyncAction[A](p: BodyParser[A] = parse.anyContent)(f: User.ID => Request[A] => Future[Result]): Action[A] =
    Action.async(p) { request => withAsyncToken(request)(_ => f) }

  private def withToken[A](request: Request[A])(f: Token => User.ID => Request[A] => Result): Result =
    tokenized(request)(f)(Unauthorized(Json.obj("err" -> "No authorization token")))

  private def withAsyncToken[A](request: Request[A])(f: Token => User.ID => Request[A] => Future[Result]): Future[Result] =
    tokenized(request)(f)(Future.successful(Unauthorized(Json.obj("err" -> "No authorization token"))))

  private def tokenized[A, B](request: Request[A])(f: Token => User.ID => Request[A] => B)(orElse: B): B =
    authHeader(request).flatMap(
      token => Cache.get(token).map(id => f(token)(id.toString.toLong)(request))
    ).getOrElse(orElse)

  private def authHeader[A](request: Request[A]): Option[Token] =
    request.headers.get(authTokenHeader).orElse(request.getQueryString(authTokenURLKey))

}
