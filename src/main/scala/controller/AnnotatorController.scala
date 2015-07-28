package es.uvigo.ei.sing.biomsef
package controller

import scala.concurrent.Future

import play.api.Play
import play.api.libs.json.{ Json, JsValue }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import entity._
import annotator._

object AnnotatorController extends Controller with Authorization {

  lazy val annotator = GlobalSettings.annotator

  def annotateOne(id: Article.ID): Action[AnyContent] =
    AuthorizedAction(parse.anyContent) { _ => _ =>
      annotator ! Annotate(id)
      Accepted
    }

  def annotate: Action[JsValue] =
    AuthorizedAction(parse.json) { _ => request =>
      (request.body \ "ids").validate[Set[Article.ID]].fold(
        errors => BadRequest(Json.obj("err" -> errors.mkString("\n"))),
        ids    => { ids.foreach(id => annotator ! Annotate(id)); Accepted }
      )
    }

}
