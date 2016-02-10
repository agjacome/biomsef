package es.uvigo.ei.sing.biomsef.entity

import play.api.libs.json._

import es.uvigo.ei.sing.biomsef.BaseSpec
import es.uvigo.ei.sing.biomsef.entity.Generators._

class KeywordSpec extends BaseSpec {

  private[this] def createJson(keyword : Keyword) =
    JsObject(Seq(
      keyword.id map ("id" -> Json.toJson(_))
    ).flatten ++ Seq(
      "normalized"  -> JsString(keyword.normalized.toString),
      "category"    -> JsString(keyword.category.toString),
      "occurrences" -> JsNumber(keyword.occurrences.value)
    ))

  "A Keyword" - {

    "can be constructed" - {

      "with an optional Keyword ID, a normalized text Sentence, a Category and the number of occurrences" in {
        forAll(keywordTupleGenerator) { case (id, normalized, category, occurrences) =>
          Keyword(id, normalized, category, occurrences) should have (
            'id          (id),
            'normalized  (normalized),
            'category    (category),
            'occurrences (occurrences.value)
          )
        }
      }

    }

    "can be transformed to a JSON object" in {
      forAll(keywordGenerator) { keyword : Keyword =>
        (Json toJson keyword) should equal (createJson(keyword))
      }
    }

  }

}

