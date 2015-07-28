package es.uvigo.ei.sing

import java.io.InputStream
import java.nio.file.{ Path, Paths }

import scala.annotation.tailrec

import play.api.Application

package object biomsef {

  def resourceStream(resourceName: String): InputStream =
    getClass.getResourceAsStream(resourceName)

  def httpContext(implicit app: Application): String =
    app.configuration.getString("play.http.context") map {
      case path if path endsWith "/" => path
      case path                      => path + "/"
    } getOrElse "/"

  def dataDir(implicit app: Application): Path =
    (app.configuration.getString("datadir").fold(Paths get "share") {
      str => Paths get str
    }).toAbsolutePath

  implicit class OptionOps[A](val opt: Option[A]) extends AnyVal {
    def getOrError(message: String): A =
      opt.getOrElse(sys.error(message))
  }

  def findAll(term: String, text: String): List[(Int, Int)] = {
    @tailrec def iter(pos: Int, acc: List[(Int, Int)]): List[(Int, Int)] =
      if (pos >= text.length - 1) acc
      else Option(text.indexOf(term, pos)).filter(_ > 0) match {
        case Some(start) =>
          val end = start + term.length
          iter(pos + end, (start, end) :: acc)
        case None => acc
      }

    iter(0, List.empty)
  }

}
