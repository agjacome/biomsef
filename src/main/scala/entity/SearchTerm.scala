package es.uvigo.ei.sing.biomsef
package entity

import play.api.libs.json._
import play.api.libs.functional.syntax._

final case class SearchTerm (
  term:      String,
  tf:        Double,
  idf:       Double,
  tfidf:     Double,
  articleId: Article.ID,
  keywordId: Keyword.ID
) {

  def id: SearchTerm.ID = (term, articleId, keywordId)

}

object SearchTerm extends ((String, Double, Double, Double, Article.ID, Keyword.ID) => SearchTerm) {

  type ID = (String, Article.ID, Keyword.ID)

  implicit val SearchTermWrites: Writes[SearchTerm] = (
    (__ \ 'term).write[String] and
    (__ \ 'score).write[Double] and
    (__ \ 'articleId).write[Article.ID] and
    (__ \ 'keywordId).write[Keyword.ID]
  )(s => (s.term, s.tfidf, s.articleId, s.keywordId))

}
