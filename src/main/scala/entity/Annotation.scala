package es.uvigo.ei.sing.biomsef
package entity

import play.api.libs.json._

final case class Annotation (
  id:        Option[Annotation.ID],
  articleId: Article.ID,
  keyword:   Keyword.ID,
  text:      String,
  start:     Long,
  end:       Long
)

object Annotation extends ((Option[Long], Article.ID, Keyword.ID, String, Long, Long) => Annotation) {

  type ID = Long

  implicit val AnnotationWrites: Writes[Annotation] = Json.writes[Annotation]

}

final case class AnnotatedArticle (
  article:     Article,
  annotations: Set[Annotation],
  keywords:    Set[Keyword]
)

object AnnotatedArticle extends ((Article, Set[Annotation], Set[Keyword]) => AnnotatedArticle) {

  implicit val AnnotatedArticleWrites: Writes[AnnotatedArticle] = Json.writes[AnnotatedArticle]

}
