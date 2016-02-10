package es.uvigo.ei.sing.biomsef.annotator

import akka.actor.{ PoisonPill, Props }
import play.api.test.WithApplication

import es.uvigo.ei.sing.biomsef.ActorBaseSpec
import es.uvigo.ei.sing.biomsef.entity._
import es.uvigo.ei.sing.biomsef.database.DatabaseProfile

class AnnotatorSpec extends ActorBaseSpec {

  private[this] lazy val expectations = Table(
    ("document", "keywords", "annotations"),
    (
      (
        cleanSpaces(
          """|Additive effects of orexin B and vasoactive intestinal polypeptide
             |on LL-37-mediated antimicrobial activities"""
        ), cleanSpaces(
          """|The present study examined the bactericidal effects of orexin B
             |(ORXB) and vasoactive intestinal peptide (VIP) alone or combined
             |with cationic antimicrobial peptides, such as LL-37, on
             |Escherichia coli, Pseudomonas aeruginosa, Streptococcus mutans
             |and Staphylococcus aureus. The bactericidal effect of ORXB or VIP
             |alone was detected in low NaCl concentration, but attenuated in
             |physiological NaCl concentration (150 mM). However, such
             |attenuated bactericidal activities of ORXB and VIP in 150 mM NaCl
             |were regained by adding LL-37. Therefore, our results indicate
             |that VIP and ORXB appear to mediate bactericidal effects in
             |concert with LL-37 in the physiological context of mucosal tissue."""
        )
      ),
      Set(
        (Sentence("orexin B"),                      Protein,  Size(1)),
        (Sentence("LL-37"),                         Protein,  Size(2)),
        (Sentence("ORXB or VIPalone"),              DNA,      Size(1)),
        (Sentence("ORXB"),                          Protein,  Size(2)),
        (Sentence("Pseudomonas aeruginosa"),        Species,  Size(1)),
        (Sentence("Staphylococcus aureus"),         Species,  Size(1)),
        (Sentence("InChI=1S/ClH.Na/h1H;/q;+1/p-1"), Compound, Size(2))
      ),
      Set(
        (Sentence("orexin B"),               Position( 55), Position( 63)),
        (Sentence("LL-37"),                  Position(173), Position(178)),
        (Sentence("ORXB or VIPalone"),       Position(298), Position(314)),
        (Sentence("ORXB"),                   Position(466), Position(470)),
        (Sentence("ORXB"),                   Position(568), Position(572)),
        (Sentence("LL-37"),                  Position(627), Position(632)),
        (Sentence("Pseudomonas aeruginosa"), Position(200), Position(222)),
        (Sentence("Staphylococcus aureus"),  Position(248), Position(269)),
        (Sentence("NaCl"),                   Position(335), Position(339)),
        (Sentence("NaCl"),                   Position(386), Position(390))
      )
    )
  )

  "The top-level Annotator" - {

    forAll(expectations) { (document, keywords, annotations) =>

      s"should annotate correctly Document '${document._1}'" in new WithApplication {
        val annotator = system.actorOf(Props[Annotator], "Annotator")
        val database  = DatabaseProfile()
        implicit val session = database.createSession()

        import database._
        import database.profile.simple._

        val documentId = Documents returning Documents.map(_.id) += Document(
          None, document._1, document._2
        )

        annotator ! Annotate(documentId)
        expectNoMsg(waitTime)
        annotator ! PoisonPill

        val storedKeywords = (Keywords map { k => (k.normalized, k.category, k.occurrences) }).list
        storedKeywords should contain theSameElementsAs keywords

        val storedAnnotations = (Annotations map { a => (a.text, a.startPosition, a.endPosition) }).list
        storedAnnotations should contain theSameElementsAs annotations

        (Documents filter (_.id is documentId) map (_.blocked)).first should be (false)
        (Documents filter (_.id is documentId) map (_.annotated)).first should be (true)

        session.close()
      }

      s"should not annotate Document '${document._1}' if marked as already annotated" in new WithApplication {
        val annotator = system.actorOf(Props[Annotator], "Annotator")
        val database  = DatabaseProfile()
        implicit val session = database.createSession()

        import database._
        import database.profile.simple._

        val documentId = Documents returning Documents.map(_.id) += Document(
          None, document._1, document._2, annotated = true
        )

        annotator ! Annotate(documentId)
        expectNoMsg(waitTime)
        annotator ! PoisonPill

        Keywords.list    should be ('empty)
        Annotations.list should be ('empty)

        session.close()
      }

      s"should not annotate Document '${document._1}' if marked as blocked" in new WithApplication {
        val annotator = system.actorOf(Props[Annotator], "Annotator")
        val database  = DatabaseProfile()
        implicit val session = database.createSession()

        import database._
        import database.profile.simple._

        val documentId = Documents returning Documents.map(_.id) += Document(
          None, document._1, document._2, blocked = true
        )

        annotator ! Annotate(documentId)
        expectNoMsg(waitTime)
        annotator ! PoisonPill

        Keywords.list    should be ('empty)
        Annotations.list should be ('empty)

        session.close()
      }

    }

  }

}

