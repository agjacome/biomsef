package es.uvigo.ei.sing.biomsef

import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.testkit.TestKitBase

import org.scalatest.BeforeAndAfterAll

trait ActorBaseSpec extends BaseSpec with TestKitBase with BeforeAndAfterAll {

  // required because ImplicitSender is incompatible with TestKitBase trait, it
  // has a self-type with TestKit class: this has been fixed in Akka 2.3, but
  // that version is incompatible with Play 2.2.x, we're stuck with Akka 2.2.x
  // and this bug by now
  implicit def self = testActor

  lazy val system   = ActorSystem("testSystem")
  lazy val waitTime = 20.seconds

  override def afterAll( ) = shutdown(system)

}

