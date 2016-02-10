package es.uvigo.ei.sing.biomsef.entity

import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen

object Generators {

  lazy val nonEmptyStringGenerator = Gen.alphaStr suchThat {
    str => str.trim split "\\s+" forall (!_.isEmpty)
  }

  lazy val documentGenerator = documentTupleGenerator map Document.tupled

  lazy val documentTupleGenerator = for {
    id        <- arbitrary[Option[Long]] map (_ map DocumentId)
    title     <- nonEmptyStringGenerator map Sentence
    text      <- nonEmptyStringGenerator
    pubmedId  <- arbitrary[Option[Long]] map (_ map PubMedId)
    annotated <- arbitrary[Boolean]
    blocked   <- arbitrary[Boolean]
  } yield (id, title, text, pubmedId, annotated, blocked)

  lazy val keywordGenerator = keywordTupleGenerator map Keyword.tupled

  lazy val keywordTupleGenerator = for {
    id          <- arbitrary[Option[Long]] map (_ map KeywordId)
    normalized  <- nonEmptyStringGenerator map Sentence
    category    <- Gen.oneOf(Seq(Compound, Drug, Gene, Protein, Species, DNA, RNA, CellLine, CellType))
    occurrences <- Gen.choose(0, Long.MaxValue) map Size
  } yield (id, normalized, category, occurrences)

  lazy val annotationGenerator = annotationTupleGenerator map Annotation.tupled

  lazy val annotationTupleGenerator = for {
    id            <- arbitrary[Option[Long]] map (_ map AnnotationId)
    documentId    <- arbitrary[Long] map DocumentId
    keywordId     <- arbitrary[Long] map KeywordId
    startPosition <- Gen.choose(0, Long.MaxValue / 2) map Position
    endPosition   <- Gen.choose(startPosition.value, Long.MaxValue) map Position
    text          <- nonEmptyStringGenerator map Sentence
  } yield (id, documentId, keywordId, text, startPosition, endPosition)

}

