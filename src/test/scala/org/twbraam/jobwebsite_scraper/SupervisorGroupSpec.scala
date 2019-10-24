package org.twbraam.jobwebsite_scraper

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.{Assertion, WordSpecLike}
import org.twbraam.jobwebsite_scraper.SupervisorGroup.ScrapeWebsiteResponse
import org.twbraam.jobwebsite_scraper.websites._

import scala.collection.immutable.ListMap
import scala.concurrent.duration._

class SupervisorGroupSpec extends ScalaTestWithActorTestKit with WordSpecLike {


  import SupervisorGroup._
  "SupervisorGroup" must {
    "Return a non-empty Set for Archer and FunctionalWorks" in {
      groupSpec(List(FunctionalWorks, Archer))
    }

  }

  def groupSpec(websites: List[Website]): Assertion = {
    val probe = createTestProbe[ScrapeWebsiteResponse]()
    websites.foreach(website => spawn(SupervisorGroup.init(website, probe.ref), s"SupervisorGroup-$website"))

    val response1: Map[String, Int] = probe.receiveMessage(600.seconds).scrapeResults
    val response2: Map[String, Int] = probe.receiveMessage(600.seconds).scrapeResults
    probe.stop

    val consolidated = (response1.keySet ++ response2.keySet).map { i => (i, response1.getOrElse(i, 0) + response2.getOrElse(i, 0)) }.toMap

    println(s"Result: ${ListMap(consolidated.toSeq.sortWith(_._2 > _._2): _*)}")

    consolidated shouldBe a[Map[_, _]]
    consolidated should not be empty
  }

}