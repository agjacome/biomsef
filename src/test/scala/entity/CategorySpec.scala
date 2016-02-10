package es.uvigo.ei.sing.biomsef.entity

import play.api.libs.json._
import org.scalacheck.Arbitrary.arbitrary

import es.uvigo.ei.sing.sds.BaseSpec

class CategorySpec extends BaseSpec {

  private[this] lazy val validCategoryIds = Table(
    ("id"          , "cat"   ),
    (CategoryId(1) , Compound),
    (CategoryId(2) , Drug    ),
    (CategoryId(3) , Gene    ),
    (CategoryId(4) , Protein ),
    (CategoryId(5) , Species ),
    (CategoryId(6) , DNA     ),
    (CategoryId(7) , RNA     ),
    (CategoryId(8) , CellLine),
    (CategoryId(9) , CellType)
  )

  private[this] lazy val invalidCategoryIds = arbitrary[Long] suchThat {
    num => !(validCategoryIds map (_._1.value) contains num)
  } map CategoryId

  private[this] lazy val validCategoryStr = Table(
    ("str"      , "cat"   ),
    ("Compound" , Compound),
    ("Drug"     , Drug    ),
    ("Gene"     , Gene    ),
    ("Protein"  , Protein ),
    ("Species"  , Species ),
    ("DNA"      , DNA     ),
    ("RNA"      , RNA     ),
    ("CellLine" , CellLine),
    ("CellType" , CellType)
  )

  private[this] lazy val invalidCategoryStr = arbitrary[String] suchThat {
    str => !(validCategoryStr map (_._1) contains str)
  }

  "A Category" - {

    "can be constructed" - {

      "with a Category ID" in {
        forAll(validCategoryIds) { (id : CategoryId, cat : Category) =>
          Category(id) should be (cat)
        }
      }

      "with an String representing a Category" in {
        forAll(validCategoryStr) { (str : String, cat : Category) =>
          Category(str) should be (cat)
        }
      }

      "by parsing a JSON String" in {
        forAll(validCategoryStr) { (str : String, cat : Category) =>
          JsString(str).as[Category] should be (cat)
        }
      }

    }

    "can be converted" - {

      "to a String" in {
        forAll(validCategoryStr) { (str : String, cat : Category) =>
          cat.toString should equal (str)
        }
      }

      "to a JSON String" in {
        forAll(validCategoryStr) { (str : String, cat : Category) =>
          Json toJson cat should be (JsString(str))
        }
      }

    }

    "should throw a NoSuchElementException" - {

      "when constructed from an invalid Category ID" in {
        forAll(invalidCategoryIds) { (id : CategoryId) =>
          a [NoSuchElementException] should be thrownBy {
            val category = Category(id)
          }
        }
      }

      "when constructed from an invalid String" in {
        forAll(invalidCategoryStr) { (str : String) =>
          a [NoSuchElementException] should be thrownBy {
            val category = Category(str)
          }
        }
      }

    }

  }

}

