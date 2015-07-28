package es.uvigo.ei.sing.biomsef
package entity

import play.api.libs.json._
import play.api.mvc.{ PathBindable, QueryStringBindable }

sealed abstract class Category(val id: Category.ID)

case object Compound extends Category(1L)
case object Drug     extends Category(2L)
case object Gene     extends Category(3L)
case object Protein  extends Category(4L)
case object Species  extends Category(5L)
case object DNA      extends Category(6L)
case object RNA      extends Category(7L)
case object CellLine extends Category(8L)
case object CellType extends Category(9L)
case object Disease  extends Category(10L)

object Category {

  type ID = Long

  lazy val categories: Set[Category] =
    Set(Compound, Drug, Gene, Protein, Species, DNA, RNA, CellLine, CellType, Disease)

  lazy val fromId: Map[Long, Category] =
    categories.map(v => (v.id -> v)).toMap

  lazy val fromString: Map[String, Category] =
    categories.map(v => (v.toString.toLowerCase -> v)).toMap

  def apply(id:  Long  ): Option[Category] = fromId.get(id)
  def apply(str: String): Option[Category] = fromString.get(str.toLowerCase)

  implicit val CategoryWrites: Writes[Category] = Writes { c => JsString(c.toString) }
  implicit val CategoryReads:  Reads[Category]  = Reads.of[String].map(fromString)

  implicit val bindQuery: QueryStringBindable[Category] =
    QueryStringBindable.bindableString.transform(fromString, _.toString.toLowerCase)

  implicit def bindPath(implicit binder: PathBindable[String]): PathBindable[Category] =
    binder.transform(fromString, _.toString.toLowerCase)

}
