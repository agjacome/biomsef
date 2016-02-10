package es.uvigo.ei.sing.biomsef.entity

import play.api.libs.json._
import org.scalacheck.Gen

import es.uvigo.ei.sing.biomsef.BaseSpec
import es.uvigo.ei.sing.biomsef.entity.Generators._

class SearchResultSpec extends BaseSpec {

  private[this] lazy val searchResultGenerator  = searchResultTupleGenerator  map SearchResult.tupled

  private[this] lazy val searchResultTupleGenerator = for {
    document   <- documentGenerator
    keywordSet <- Gen.containerOf[Set, Keyword](keywordGenerator)
  } yield (document, keywordSet)

  private[this] def createJson(searchResult : SearchResult) =
    Json.obj(
      "document" -> Json.toJson(searchResult.document),
      "keywords" -> JsArray((searchResult.keywords map { k => Json toJson k }).toSeq)
    )

  "The SearchResult entity" - {

    "can be constructed with a Document and a Set of Keywords related to that Document" in {
      forAll(searchResultTupleGenerator) { case (document, keywordSet) =>
        SearchResult(document, keywordSet) should have (
          'document (document),
          'keywords (keywordSet)
        )
      }
    }

    "can be transformed to a JSON object" in {
      forAll(searchResultGenerator) { searchResult : SearchResult =>
        (Json toJson searchResult) should equal (createJson(searchResult))
      }
    }

  }

}
