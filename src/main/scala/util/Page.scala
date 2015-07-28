package es.uvigo.ei.sing.biomsef
package util

import play.api.libs.json._
import play.api.libs.functional.syntax._

final case class Page[A](items: Seq[A], page: Int, offset: Int, total: Int) {
  lazy val prev: Option[Int] = Option(page - 1).filter(_ >= 0)
  lazy val next: Option[Int] = Option(page + 1).filter(_ => (offset + items.size) < total)
}

object Page {

  implicit def PageWrites[A](implicit aWrites: Writes[A]): Writes[Page[A]] = (
    (__ \ 'items).write[Seq[A]] and
    (__ \ 'page).write[Int] and
    (__ \ 'offset).write[Int] and
    (__ \ 'total).write[Int] and
    (__ \ 'prev).writeNullable[Int] and
    (__ \ 'next).writeNullable[Int]
  )(p => (p.items, p.page, p.offset, p.total, p.prev, p.next))

}
