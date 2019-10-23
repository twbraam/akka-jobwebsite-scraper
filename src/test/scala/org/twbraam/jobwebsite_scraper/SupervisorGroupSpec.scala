package org.twbraam.jobwebsite_scraper

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike
import org.twbraam.jobwebsite_scraper.websites.FunctionalWorks

import scala.collection.immutable.ListMap
import scala.concurrent.duration._

class SupervisorGroupSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  import SupervisorGroup._
  "SupervisorGroup" must {
    "Return a non-empty Set" in {
      val probe = createTestProbe[ScrapeWebsiteResponse]()
      val worker = spawn(SupervisorGroup.init(FunctionalWorks, probe.ref), "simple")

      val response = probe.receiveMessage(60.seconds)
      println(s"Got response: ${ListMap(response.scrapeResults.toSeq.sortWith(_._2 > _._2):_*)}")
    }
  }

}