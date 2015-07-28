package es.uvigo.ei.sing.biomsef

import scala.concurrent.duration._
import scala.concurrent.Future
import scala.util.{ Success, Failure }

import akka.actor._

import play.api.{ Application, Logger, Play }
import play.api.mvc.Results._
import play.api.mvc.{ RequestHeader, Result }
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json

import annotator._
import service._

object GlobalSettings extends play.api.GlobalSettings {

  import Play.current

  lazy val annotator: ActorRef = Akka.system.actorOf(Props[Annotator], "annotator")
  lazy val indexer:   ActorRef = Akka.system.actorOf(Props[IndexerService], "indexer")

  lazy val indexerSchedule: Cancellable = {
    val delay    = current.configuration.getMilliseconds("indexer.initialDelay").getOrElse(500L).milliseconds
    val interval = current.configuration.getMilliseconds("indexer.interval"    ).getOrElse(3600000L).milliseconds
    Akka.system.scheduler.schedule(delay, interval, indexer, UpdateIndex)
  }

  // Force evaluation of lazys, cannot be created as "vals" because they need a
  // running Play application (Play.current) and that will only be available
  // when this onStart method is called, but not before.
  override def onStart(app: Application): Unit = {
    annotator
    indexerSchedule
    ()
  }

  override def onError(request: RequestHeader, err: Throwable): Future[Result] = 
    Future.successful {
      InternalServerError(Json obj ("err" -> s"Server Error: ${err.getMessage}"))
    }

  override def onHandlerNotFound(request: RequestHeader): Future[Result] =
    Future.successful(
      if (request.path == httpContext)
        MovedPermanently(request.path.dropRight(1))
      else
        NotFound(Json.obj("err" -> s"Path not found: ${request.path}"))
    )

  override def onBadRequest(request : RequestHeader, error : String): Future[Result] =
    Future.successful(BadRequest(Json.obj("err" -> s"Bad Request: $error")))

}
