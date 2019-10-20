package org.twbraam.scraper42

import java.net.URL

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike
import org.twbraam.scraper42.Supervisor
import org.twbraam.scraper42.websites.FunctionalWorks
import scala.concurrent.duration._

import scala.collection.Set

class SupervisorSpec extends ScalaTestWithActorTestKit with WordSpecLike {

  "Supervisor" must {
    "Return a non-empty Set" in {
      val probe = createTestProbe[Nothing]()
      val worker = spawn(Supervisor.init(FunctionalWorks), "simple")

      val response = probe.receiveMessage(10.seconds)
    }
  }

}