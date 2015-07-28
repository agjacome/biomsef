package es.uvigo.ei.sing.biomsef
package controller

import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import entity._
import database._

object KeywordsController extends Controller {

  lazy val keywordsDAO = new KeywordsDAO

  def get(id: Keyword.ID): Action[AnyContent] =
    Action.async(keywordsDAO.get(id) map {
      _.map(k => Ok(Json.toJson(k))).getOrElse(NotFound(Json.obj("err" -> "Keyword not found")))
    })

}
