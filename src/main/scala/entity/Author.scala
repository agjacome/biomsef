package es.uvigo.ei.sing.biomsef
package entity

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.Form
import play.api.data.Forms._

final case class Author (
  id:        Option[Author.ID],
  lastName:  String,
  firstName: String,
  initials:  String
)

object Author extends ((Option[Long], String, String, String) => Author) {

  type ID = Long

  def apply(lastName: String, firstName: String, initials: String): Author =
    Author.apply(None, lastName, firstName, initials)

  implicit val AuthorWrites: Writes[Author] = (
    (__ \ 'id).writeNullable[Long] and
    (__ \ 'lastName).write[String] and
    (__ \ 'firstName).write[String] and
    (__ \ 'initials).write[String]
  )(unlift(Author.unapply))

}
