package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.{Assertion, WordSpecLike}
import org.twbraam.jobwebsite_scraper.KeywordScraper.{ScrapeJobRequest, ScrapeJobResponse}
import org.twbraam.jobwebsite_scraper.websites._

import scala.concurrent.duration._
import scala.collection.Set


class KeywordScraperSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  val probe = createTestProbe[ScrapeJobResponse]()



  "KeywordScraper" must {
    "Return a non-empty Set for Archer" in {
      kwspec(Archer, new URL("https://www.archer.eu/job/remote-senior-scala-engineer-2/"))
    }

    "Return a non-empty Set for FunctionalWorks" in {
      kwspec(FunctionalWorks, new URL("https://functional.works-hub.com/jobs/618"))
    }


  }

  def kwspec(website: Website, url: URL): Assertion = {
    val worker = spawn(KeywordScraper.init(website), s"testprobe-$website")
    worker ! ScrapeJobRequest(url, probe.ref)

    val response = probe.receiveMessage(10.seconds)

    println(s"Got repsonse: ${response.scrapeResults}")

    response.scrapeResults shouldBe a[Set[_]]
    response.scrapeResults should not be empty
  }

}