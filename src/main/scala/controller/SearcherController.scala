package es.uvigo.ei.sing.biomsef
package controller

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._

import entity._
import searcher._

object SearcherController extends Controller {

  lazy val searcher = new Searcher

  implicit val SearchResultWrites: Writes[(Article, Set[Keyword])] = (
    (__ \ 'article).write[Article] and
    (__ \ 'keywords).write[Set[Keyword]]
  )(s => s)

  def search(query: String, page: Option[Int], pageSize: Option[Int]): Action[AnyContent] =
    Action.async(searcher.search(query, page.getOrElse(0), pageSize.getOrElse(50)) map {
      result => Ok(Json.toJson(result))
    })

}

