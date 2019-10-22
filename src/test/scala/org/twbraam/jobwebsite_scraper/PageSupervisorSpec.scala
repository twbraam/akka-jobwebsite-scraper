package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike
import org.twbraam.jobwebsite_scraper.PageSupervisor
import org.twbraam.jobwebsite_scraper.websites.FunctionalWorks

import scala.concurrent.duration._
import scala.collection.Set
import scala.collection.immutable.ListMap

class PageSupervisorSpec extends ScalaTestWithActorTestKit with WordSpecLike {

  "Supervisor" must {
    "Return a non-empty Set" in {
      val probe = createTestProbe[PageSupervisor.ScrapePageResponse]()
      val worker = spawn(PageSupervisor.init(FunctionalWorks, probe.ref), "simple")

      val response = probe.receiveMessage(10.seconds)
      println(s"Got response: ${ListMap(response.scrapeResults.toSeq.sortWith(_._2 > _._2):_*)}")
    }
  }

}