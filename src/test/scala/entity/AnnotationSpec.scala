package es.uvigo.ei.sing.biomsef.entity

import play.api.libs.json._
import org.scalacheck.Gen

import es.uvigo.ei.sing.biomsef.BaseSpec
import es.uvigo.ei.sing.biomsef.entity.Generators._

class AnnotationSpec extends BaseSpec {

  private[this] lazy val invalidPosAnnotationTupleGenerator = for {
    endPosition   <- Gen.choose(0, Long.MaxValue / 2) map Position
    startPosition <- Gen.choose(endPosition.value + 1, Long.MaxValue) map Position
    (id, documentId, keywordId, text, _, _) <- annotationTupleGenerator
  } yield (id, documentId, keywordId, text, startPosition, endPosition)

  private[this] def createJson(annotation : Annotation) =
    JsObject(Seq(
      annotation.id map ("id" -> Json.toJson(_))
    ).flatten ++ Seq(
      "documentId"    -> JsNumber(annotation.documentId.value),
      "keywordId"     -> JsNumber(annotation.keywordId.value),
      "text"          -> JsString(annotation.text.toString),
      "startPosition" -> JsNumber(annotation.startPosition.value),
      "endPosition"   -> JsNumber(annotation.endPosition.value)
    ))

  "An Annotation" - {

    "can be constructed" - {

      "with an optional Annotation ID, a Document ID, a Keyword ID, a text Sentence, and start and ending Positions" in {
        forAll(annotationTupleGenerator) { case (id, documentId, keywordId, text, start, end) =>
          Annotation(id, documentId, keywordId, text, start, end) should have (
            'id            (id),
            'documentId    (documentId.value),
            'keywordId     (keywordId.value),
            'text          (text),
            'startPosition (start.value),
            'endPosition   (end.value)
          )
        }
      }

    }

    "can be transformed to a JSON object" in {
      forAll(annotationGenerator) { annotation : Annotation =>
        (Json toJson annotation) should equal (createJson(annotation))
      }
    }

    "should throw an IllegalArgumentException" - {

      "when constructed with an Starting Position bigger than the Ending Position" in {
        forAll(invalidPosAnnotationTupleGenerator) { case (id, documentId, keywordId, text, start, end) =>
          a [IllegalArgumentException] should be thrownBy {
            val annotation = Annotation(id, documentId, keywordId, text, start, end)
          }
        }
      }

    }


  }

}

