package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike
import org.twbraam.jobwebsite_scraper.KeywordScraper.{ScrapePageRequest, ScrapePageResponse}
import org.twbraam.jobwebsite_scraper.websites.FunctionalWorks
import scala.concurrent.duration._

import scala.collection.Set

class KeywordScraperSpec extends ScalaTestWithActorTestKit with WordSpecLike {

  "KeywordScraper" must {
    "Return a non-empty Set" in {
      val probe = createTestProbe[ScrapePageResponse]()
      val worker = spawn(KeywordScraper.init(FunctionalWorks), "simple")

      worker ! ScrapePageRequest(new URL("https://functional.works-hub.com/jobs/618"), probe.ref)

      val response = probe.receiveMessage(10.seconds)
      response.scrapeResults shouldBe a[Set[_]]
      response.scrapeResults should not be empty
    }
  }

}