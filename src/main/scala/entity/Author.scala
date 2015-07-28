package es.uvigo.ei.sing.biomsef
package entity

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.Form
import play.api.data.Forms._

final case class Author (
  id:        Option[Author.ID],
  pubmedId:  Option[Author.PMID],
  lastName:  String,
  firstName: String,
  initials:  String
)

object Author extends ((Option[Long], Option[Long], String, String, String) => Author) {

  type ID   = Long
  type PMID = Long

  def apply(pubmedId: Option[PMID], lastName: String, firstName: String, initials: String): Author =
    Author.apply(None, pubmedId, lastName, firstName, initials)

  implicit val AuthorWrites: Writes[Author] = (
    (__ \ 'id).writeNullable[Long] and
    (__ \ 'pubmedId).writeNullable[Long] and
    (__ \ 'lastName).write[String] and
    (__ \ 'firstName).write[String] and
    (__ \ 'initials).write[String]
  )(unlift(Author.unapply))

  implicit val AuthorForm: Form[Author] = Form {
    mapping(
      "pubmedId" -> optional(longNumber(min = 1L)),
      "lastName" -> nonEmptyText,
      "firstName" -> nonEmptyText,
      "initials" -> nonEmptyText
    )(Author.apply)(a => Some((a.pubmedId, a.lastName, a.firstName, a.initials)))
  }

}
