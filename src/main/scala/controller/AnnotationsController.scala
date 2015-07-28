package es.uvigo.ei.sing.biomsef
package controller

import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import entity._
import database._

object AnnotationsController extends Controller {

  lazy val annotationsDAO = new AnnotationsDAO

  def get(id: Annotation.ID): Action[AnyContent] =
    Action.async(annotationsDAO.get(id) map {
      _.map(a => Ok(Json.toJson(a))).getOrElse(NotFound(Json.obj("err" -> "Annotation not found")))
    })

}
