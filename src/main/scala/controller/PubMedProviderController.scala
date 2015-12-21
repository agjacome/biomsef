package es.uvigo.ei.sing.biomsef
package controller

import scala.concurrent.Future
import scala.util.control.NonFatal

import play.api.Play
import play.api.libs.json.{ Json, JsValue }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import entity._
import provider._

object PubMedProviderController extends Controller with Authorization {

  lazy val pubmed = new PubMedProvider

  def search(query: String, limit: Option[Int], page: Option[Int], count: Option[Int]): Action[AnyContent] =
    AuthorizedAsyncAction(parse.anyContent) { _ => _ =>
      pubmed.search(query, limit, page.getOrElse(0), count.getOrElse(50)) map {
        results => Ok(Json.toJson(results))
      } recover {
        case NonFatal(e) => InternalServerError(Json.obj("err" -> e.getMessage))
      }
    }

  def download: Action[JsValue] =
    AuthorizedAsyncAction(parse.json) { _ => request => 
      (request.body \ "ids").validate[Set[Article.PMID]].fold(
        errors => Future.successful(BadRequest(Json.obj("err" -> errors.mkString("\n")))),
        ids    => pubmed.download(ids).map(articles => Ok(Json.toJson(articles.map(_.id))))
      )
    }

}
