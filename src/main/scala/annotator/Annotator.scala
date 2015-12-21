package es.uvigo.ei.sing.biomsef
package annotator

import scala.collection.concurrent.TrieMap
import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import play.api.{ Configuration, Logger, Play }

import akka.actor._

import entity._
import database._

sealed trait AnnotatorMessage { def articleId: Article.ID }
final case class Annotate (articleId: Article.ID) extends AnnotatorMessage
final case class Finished (articleId: Article.ID) extends AnnotatorMessage
final case class Failed   (articleId: Article.ID, cause: Throwable) extends AnnotatorMessage

final class Annotator extends Actor {

  import context._

  lazy val articlesDAO    = new ArticlesDAO
  lazy val keywordsDAO    = new KeywordsDAO
  lazy val annotationsDAO = new AnnotationsDAO

  private lazy val annotators = createAnnotators(Play.current.configuration.getConfig("annotator"))
  private lazy val completed  = TrieMap.empty[Article.ID, (Int, Boolean)]

  override def receive: Receive = {
    case Annotate(id)    => annotate(id, sender.path.name)
    case Finished(id)    => finished(id, sender.path.name)
    case Failed(id, err) => failed(id, err, sender.path.name)
  }

  def annotate(id: Article.ID, senderName: String): Unit = {
    Logger.info(s"Annotating article $id")
    articlesDAO.get(id) onComplete {
      case Success(article) => checkAndAnnotate(article, id, senderName)
      case Failure(error)   => failed(id, error, senderName)
    }
  }

  def finished(id: Article.ID, senderName: String): Unit = {
    Logger.info(s"[$senderName] Finished annotating article $id")
    updateCompleted(id, false)
    if (allAnnotatorsCompleted(id)) wrapUp(id)
  }

  def failed(id: Article.ID, cause: Throwable, senderName: String): Unit = {
    Logger.error(s"[$senderName] Failed annotation of article $id", cause)
    updateCompleted(id, true)
    if (allAnnotatorsCompleted(id)) wrapUp(id)
  }

  private def checkAndAnnotate(maybeArticle: Option[Article], id: Article.ID, senderName: String): Unit =
    maybeArticle.fold(Logger.error(s"[$senderName] Cannot find article $id")) {
      case Article(_, _, _, _, _, true, _) => Logger.warn(s"[$senderName] Ignoring already annotated article $id")
      case Article(_, _, _, _, _, _, true) => Logger.warn(s"[$senderName] Ignoring already processing article $id")
      case article                      => annotateArticle(article, senderName)
    }

  // FIXME: unsafe gets
  private def annotateArticle(article: Article, senderName: String): Unit =
    articlesDAO.update(article, false, true) onComplete {
      case Failure(err) => Logger.error(s"[$senderName] Cannot update article ${article.id.get} status")
      case Success(_)   => {
        completed.put(article.id.get, (0, false))
        annotators foreach (_ ! Annotate(article.id.get))
      }
    }

  // FIXME: unsafe get
  private def wrapUp(id: Article.ID): Unit = {
    val future = {
      if (anyAnnotatorFailed(id))
        annotationsDAO.deleteAnnotationsOf(id)
      else
        articlesDAO.get(id) flatMap { a => articlesDAO.update(a.get, true, false) }
    }

    completed.remove(id)

    future onComplete {
      case Success(_) => Logger.info(s"Completed annotation of article $id")
      case Failure(e) => Logger.error(s"Could not complete annotation of article $id", e)
    }
  }

  private def allAnnotatorsCompleted(id: Article.ID): Boolean =
    completed(id)._1 == annotators.size

  private def anyAnnotatorFailed(id: Article.ID): Boolean =
    completed(id)._2

  private def updateCompleted(id: Article.ID, hasFailed: Boolean = false): Unit = {
    val (counter, failed) = completed(id)
    completed.update(id, (counter + 1, failed || hasFailed))
  }

  private def createAnnotators(annotatorsConfig: Option[Configuration]): Set[ActorRef] =
    annotatorsConfig.fold(Set.empty[ActorRef]) {
      config => config.keys map {
        key => context.actorOf(Props(Class.forName(config.getString(key).get)), key)
      }
    }

}
