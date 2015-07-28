package es.uvigo.ei.sing.biomsef
package annotator

import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import akka.actor._
import akka.pattern.pipe

import play.api.cache.Cache
import play.api.Play

import entity._
import database._

trait AnnotatorAdapter extends Actor {

  import context._

  lazy val articlesDAO    = new ArticlesDAO
  lazy val keywordsDAO    = new KeywordsDAO
  lazy val annotationsDAO = new AnnotationsDAO

  override def receive: Receive = {
    case Annotate(id) => respondToSender(id, sender)
  }

  def annotate(article: Article): Future[Unit]

  private def respondToSender(id: Article.ID, sender: ActorRef): Unit =
    getArticle(id).map(annotate) onComplete {
      case Success(_)   => sender ! Finished(id)
      case Failure(err) => sender ! Failed(id, err)
    }

  private def getArticle(id: Article.ID): Future[Article] =
    articlesDAO.get(id).flatMap(_.fold(Future.failed[Article] {
      new IllegalArgumentException(s"Cannot find article with id $id")
    })(Future.successful))

  protected final def getOrStoreKeyword(normalized: String, category: Category): Future[Keyword] =
    keywordsDAO.getByNormalizedOrInsert(normalized, Keyword(None, normalized, category))

}
