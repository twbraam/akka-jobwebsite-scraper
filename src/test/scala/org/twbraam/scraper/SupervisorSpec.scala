package org.twbraam.scraper

import java.net.URL

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike

import scala.concurrent.duration._

class SupervisorSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  import Manager._
  import Supervisor._
  import Scraper._

  "Supervisor actor" must {
    "reply with empty reading if no temperature is known" in {
      val probe = createTestProbe[ScrapeSiteResponse]()
      val supervisorActor = spawn(Supervisor(
        List(new URL("https://www.google.com"))
      ))

      supervisorActor ! Supervisor.ScrapeSiteResponse(
        new URL("https://www.google.com"),
        Map("Monkey" -> 3, "Bear" -> 42))
      val response = probe.receiveMessage()
      response.url should ===(new URL("https://www.google.com"))
      response.response should ===(Map("Monkey" -> 3, "Bear" -> 42))
    }
  }
}