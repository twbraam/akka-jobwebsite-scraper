package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.{Assertion, WordSpecLike}
import org.twbraam.jobwebsite_scraper.ScraperSupervisor._
import org.twbraam.jobwebsite_scraper.websites._

import scala.collection.immutable.ListMap
import scala.concurrent.duration._

class ScraperSupervisorSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  "Supervisor" must {
    "Return a non-empty Set for Archer" in {
      supervisorSpec(Archer, new URL("https://www.archer.eu/scala-jobs/"))
    }

    "Return a non-empty Set for FunctionalWorks" in {
      supervisorSpec(FunctionalWorks, new URL("https://functional.works-hub.com/scala-jobs"))
    }
  }

  def supervisorSpec(website: Website, url: URL): Assertion = {
    val probe = createTestProbe[ScrapePageResponse](website.toString)
    val worker = spawn(ScraperSupervisor.init(website, probe.ref), s"testprobe-$website")

    worker ! ScrapePageRequest(url)

    val response = probe.receiveMessage(30.seconds)
    probe.stop

    response match {
      case ScrapePageResponse(results) =>
        println(s"Got response: ${ListMap(results.toSeq.sortWith(_._2 > _._2): _*)}")
        response.scrapeResults shouldBe a[Map[_, _]]
        response.scrapeResults should not be empty
    }
  }
}
