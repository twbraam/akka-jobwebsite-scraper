package org.twbraam.jobwebsite_scraper

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike
import org.twbraam.jobwebsite_scraper.websites.FunctionalWorks

import scala.collection.immutable.ListMap
import scala.concurrent.duration._

class ScraperSupervisorSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  import ScraperSupervisor._
  "Supervisor" must {
    "Return a non-empty Set" in {
      val probe = createTestProbe[ScrapePageResponse]()
      val worker = spawn(ScraperSupervisor.init(FunctionalWorks.url, FunctionalWorks, probe.ref), "simple")

      val response = probe.receiveMessage(30.seconds)
      println(s"Got response: ${ListMap(response.scrapeResults.toSeq.sortWith(_._2 > _._2):_*)}")
    }
  }

}