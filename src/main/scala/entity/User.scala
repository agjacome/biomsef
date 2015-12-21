package es.uvigo.ei.sing.biomsef
package entity

import play.api.Application
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.data.Form
import play.api.data.Forms._

final case class User (
  id:    Option[User.ID],
  email: String,
  pass:  String
)

object User extends ((Option[Long], String, String) => User) {

  type ID = Long

  def apply(email: String, pass: String): User =
    User.apply(None, email, pass)

  def defaultUser(app: Application): Option[User] =
    for {
      mail <- app.configuration.getString("admin.email")
      pass <- app.configuration.getString("admin.pass")
    } yield User(mail, pass)

  implicit val UserWrites: Writes[User] = (
    (__ \ 'id).writeNullable[Long] and
    (__ \ 'email).write[String]
  )(user => (user.id, user.email))

  implicit val UserForm: Form[User] = Form {
    mapping(
      "email" -> email, "pass" -> nonEmptyText
    )(User.apply)(u => Some((u.email, u.pass)))
  }

}
