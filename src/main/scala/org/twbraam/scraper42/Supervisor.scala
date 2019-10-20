package org.twbraam.scraper42

import java.net.URL

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import akka.actor.typed.SpawnProtocol
import org.twbraam.scraper42.websites.{FunctionalWorks, TraversePage, Website}

import scala.collection.immutable.ListMap

object Supervisor {
  sealed trait SupervisorMessage
  final case class ScrapeRequest(url: URL, replyTo: ActorRef[ScrapeResponse]) extends SupervisorMessage
  final case class ScrapeResponse(scrapeResults: Set[String]) extends SupervisorMessage

  import KeywordScraper._

  def init(website: Website): Behavior[ScrapePageResponse] = createScrapers(website, website.url)

  private def createScrapers(website: Website, pageUrl: URL): Behavior[ScrapePageResponse] =
    Behaviors.setup { context =>
      val jobPageLinks = TraversePage.extractLinks(pageUrl)

      val kwScraper = Behaviors.supervise(KeywordScraper.init(website)).onFailure[Exception](SupervisorStrategy.restart)
      val pool = Routers.pool(poolSize = 4)(kwScraper)
      val router = context.spawn(pool, "worker-pool")

      jobPageLinks.foreach { url =>
        router ! ScrapePageRequest(url, context.self.ref)
      }

      awaitResults(website, Map.empty)
    }


  private def awaitResults(website: Website, acc: Map[String, Int])
  : Behavior[ScrapePageResponse] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapePageResponse(response) =>
          context.log.info(s"Got result: $response")

          val newAcc = response.foldLeft(acc)((acc, n) => {
            if (acc.isDefinedAt(n)) acc.updated(n, acc(n) + 1)
            else acc.updated(n, 1)
          })

          context.log.info(s"Current acc: ${ListMap(newAcc.toSeq.sortWith(_._2 > _._2):_*)}")
          awaitResults(website, newAcc)
      }
    }
}
