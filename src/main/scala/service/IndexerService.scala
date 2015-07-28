package es.uvigo.ei.sing.biomsef
package service

import scala.concurrent.Future
import scala.util.{ Try, Success, Failure }

import play.api.{ Logger, Play }
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import akka.actor._

import entity._
import database._

sealed trait IndexerMessage
case object ClearIndex    extends IndexerMessage
case object PopulateIndex extends IndexerMessage
case object UpdateIndex   extends IndexerMessage

final class IndexerService extends Actor {

  lazy val indexDAO       = new SearchTermsDAO
  lazy val articlesDAO    = new ArticlesDAO
  lazy val keywordsDAO    = new KeywordsDAO
  lazy val annotationsDAO = new AnnotationsDAO

  override val receive: Receive = {
    case ClearIndex    => clearIndex()
    case PopulateIndex => populateIndex()
    case UpdateIndex   => updateIndex()
  }

  def clearIndex(): Unit = {
    Logger.info("Clearing search index")
    indexDAO.clear onComplete {
      case Success(_) => Logger.info("Finished clearing search index")
      case Failure(e) => Logger.error("Error clearing search index", e); sys.exit(1)
    }
  }

  def populateIndex(): Unit = {
    Logger.info("Populating search index")
    insertSearchTerms() onComplete {
      case Success(ts) => Logger.info(s"Finished populating search index with ${ts.size} entries")
      case Failure(e)  => Logger.error("Error populating search index", e); sys.exit(1)
    }
  }

  def updateIndex(): Unit = {
    Logger.info("Updating search index")
    val future: Future[Int] = for {
      terms <- searchTerms
      _     <- indexDAO.clear
      nts   <- indexDAO.insert(terms: _*).map(_.size)
    } yield nts

    future onComplete {
      case Success(nts) => Logger.info(s"Finished updating search index with $nts entries")
      case Failure(e)   => Logger.error(s"Error populating search index", e); sys.exit(1)
    }
  }

  private def insertSearchTerms(): Future[Seq[SearchTerm]] =
    searchTerms flatMap {
      terms => indexDAO.insert(terms: _*)
    }

  private def searchTerms: Future[Seq[SearchTerm]] =
    frequencies flatMap {
      f => Future.sequence(f.toSeq map {
        case ((aid, kid), (tf, idf, tfidf)) =>
          // FIXME: unsafe get
          val term = keywordsDAO.get(kid).map(_.get.normalized)
          term.map(t => SearchTerm(t, tf, idf, tfidf, aid, kid))
      })
    }

  private def frequencies: Future[Map[(Article.ID, Keyword.ID), (Double, Double, Double)]] =
    for {
      n   <- articlesDAO.count
      tfs <- annotationsDAO.countByArticleAndKeyword
    } yield computeTFIDF(n, tfs)

  private def computeTFIDF(count: Int, freqs: Map[(Article.ID, Keyword.ID), Int]): Map[(Article.ID, Keyword.ID), (Double, Double, Double)] =
    freqs.toSeq map { case ((aid, kid), tf) =>
      val idf   = Math.log(freqs.count(kv => kv._1._2 == kid).toDouble)
      val tfidf = tf * idf
      ((aid, kid), (tf.toDouble, idf, tfidf))
    } toMap

}
