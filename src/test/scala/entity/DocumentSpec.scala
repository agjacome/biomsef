package es.uvigo.ei.sing.biomsef.entity

import play.api.libs.json._
import org.scalacheck.Gen

import es.uvigo.ei.sing.biomsef.BaseSpec
import es.uvigo.ei.sing.biomsef.entity.Generators._

class DocumentSpec extends BaseSpec {

  private[this] lazy val emptyTextDocumentTupleGenerator = for {
    (id, title, _, pubmedId, annotated, blocked) <- documentTupleGenerator
  } yield (id, title, "", pubmedId, annotated, blocked)

  private[this] def createJson(document : Document) =
    JsObject(Seq(
      document.id       map ("id"       -> Json.toJson(_)),
      document.pubmedId map ("pubmedId" -> Json.toJson(_))
    ).flatten ++ Seq(
      "title"     -> JsString(document.title.toString),
      "text"      -> JsString(document.text),
      "annotated" -> JsBoolean(document.annotated),
      "blocked"   -> JsBoolean(document.blocked)
    ))

  "A Document" - {

    "can be constructed" - {

      "with an optional Document ID, a title Sentence, a text, an optional PubMed ID and annotated and blocked flags" in {
        forAll(documentTupleGenerator) { case (id, title, text, pubmedId, annotated, blocked) =>
          Document(id, title, text, pubmedId, annotated, blocked) should have (
            'id        (id),
            'title     (title),
            'text      (text),
            'pubmedId  (pubmedId),
            'annotated (annotated),
            'blocked   (blocked)
          )
        }
      }

    }

    "can be transformed to a JSON object" in {
      forAll(documentGenerator) { document : Document =>
        (Json toJson document) should equal (createJson(document))
      }
    }

    "should throw an IllegalArgumentException" - {

      "whenever constructed with an empty text" in {
        forAll(emptyTextDocumentTupleGenerator) { case (id, title, text, pubmedId, annotated, blocked) =>
          a [IllegalArgumentException] should be thrownBy {
            val document = Document(id, title, text, pubmedId, annotated, blocked)
          }
        }
      }

    }

  }

}

