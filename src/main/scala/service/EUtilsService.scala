package es.uvigo.ei.sing.biomsef
package service

import scala.concurrent.Future
import scala.xml.{ Elem => XMLElement, Node => XMLNode }

import play.api.libs.concurrent.Execution.Implicits.defaultContext

import dispatch.{ url, Http, Res }

import entity._
import util.Page

// TODO: parse List[Author] for each Article
final class EUtilsService {

  implicit val asXML: Res => XMLElement = dispatch.as.xml.Elem

  def searchArticlePMID(query: String, days: Option[Int] = None, page: Int = 0, pageSize: Int = 30): Future[Page[Article.PMID]] = {
    val offset = page * pageSize
    search("pubmed", query, days, offset, pageSize) map {
      result => Page(idList(result), page, offset, count(result))
    }
  }

  def fetchPubMedArticles(pmids: Set[Article.PMID]): Future[Set[(Article, List[Author])]] =
    fetch("pubmed", pmids, "abstract").map(parseResultArticles).map(_.toSet)

  def fetchTaxonomyScientificName(taxonomyId: Long): Future[Option[String]] =
    summary("taxonomy", taxonomyId).map(parseScientificName)

  def serviceURL(service: String): String =
    s"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/$service.fcgi"

  def search(db: String, query: String, reldate: Option[Int], retstart: Int, retmax: Int): Future[XMLElement] =
    simpleHttpRequest(serviceURL("esearch"))(
      "db"       -> db,
      "term"     -> query,
      "retstart" -> retstart.toString,
      "retmax"   -> retmax.toString,
      "datetype" -> "edat",
      "reldate"  -> reldate.map(_.toString).getOrElse("")
    )

  def fetch(db: String, ids: Set[Long], rettype: String): Future[XMLElement] =
    simpleHttpRequest(serviceURL("efetch"))(
      "db"      -> db,
      "id"      -> ids.mkString(","),
      "rettype" -> rettype
    )

  def summary(db: String, id: Long): Future[XMLElement] =
    simpleHttpRequest(serviceURL("esummary"))(
      "db" -> db, "id" -> id.toString
    )

  private def simpleHttpRequest[A](address: String)(params: (String, String)*)(implicit asA: Res => A): Future[A] =
    Http(dispatch.url(address).setBodyEncoding("UTF-8") <<? params > asA)

  private def count(result: XMLElement): Int =
    (result \ "Count").headOption.fold(0)(_.text.toInt)

  private def idList(result: XMLElement): Seq[Article.PMID] =
    (result \ "IdList" \ "Id").map(_.text).map(_.toLong)

  private def parseResultArticles(result: XMLElement): Seq[(Article, List[Author])] =
    (result \ "PubmedArticle").map(parseArticle).flatten

  private def parseArticle(article: XMLNode): Option[(Article, List[Author])] =
    for {
      pmid  <- (article \\ "PMID").headOption.map(_.text.toLong)
      title <- (article \\ "ArticleTitle").headOption.map(_.text.dropRight(1))
      abstr <- (article \\ "AbstractText").headOption.map(_.text)
      year  <- (article \\ "PubDate" \\ "Year").headOption.map(_.text.toLong).orElse(Option(1900L))
      auths <- Option(parseAuthors(article)).orElse(Option(List.empty))
    } yield (Article(Some(pmid), title, title + "." + System.lineSeparator + abstr, year), auths)

  private def parseAuthors(article: XMLNode): List[Author] =
    ((article \\ "Author") map { auth ⇒
      for {
        last <- (auth \\ "LastName").headOption.map(_.text)
        frst <- (auth \\ "ForeName").headOption.map(_.text)
        init <- (auth \\ "Initials").headOption.map(_.text)
      } yield Author(last, frst, init)
    }).toList.flatten

  private def parseScientificName(result: XMLElement): Option[String] =
    (result \\ "Item").filter(_.attribute("Name").exists(_.text == "ScientificName")).headOption.map(_.text)

}
