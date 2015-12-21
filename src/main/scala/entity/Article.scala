package es.uvigo.ei.sing.biomsef
package entity

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.Form
import play.api.data.Forms._

final case class Article (
  id:           Option[Article.ID],
  pubmedId:     Option[Article.PMID],
  title:        String,
  content:      String,
  year:         Long,
  isAnnotated:  Boolean,
  isProcessing: Boolean
)

object Article extends ((Option[Long], Option[Long], String, String, Long, Boolean, Boolean) => Article) {

  type ID   = Long
  type PMID = Long

  def apply(pubmedId: Option[PMID], title: String, content: String, year: Long): Article =
    Article.apply(None, pubmedId, title, content, year, false, false)

  implicit val ArticleWrites: Writes[Article] = (
    (__ \ 'id).writeNullable[Long] and
    (__ \ 'pubmedId).writeNullable[Long] and
    (__ \ 'title).write[String] and
    (__ \ 'content).write[String] and
    (__ \ 'year).write[Long] and
    (__ \ 'isAnnotated).write[Boolean] and
    (__ \ 'isProcessing).write[Boolean]
  )(unlift(Article.unapply))

  implicit val ArticleForm: Form[Article] = Form {
    mapping(
      "pubmedId" -> optional(longNumber(min = 1L)),
      "title"    -> nonEmptyText,
      "content"  -> nonEmptyText,
      "year"     -> longNumber(min = 1900L)
    )(Article.apply)(a => Some((a.pubmedId, a.title, a.content, a.year)))
  }

}
