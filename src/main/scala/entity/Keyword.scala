package es.uvigo.ei.sing.biomsef
package entity

import play.api.libs.json._

final case class Keyword (
  id:         Option[Keyword.ID],
  normalized: String,
  category:   Category
)

object Keyword extends ((Option[Long], String, Category) => Keyword) {

  type ID = Long

  implicit val KeywordWrites: Writes[Keyword] = Json.writes[Keyword]

}
