package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import org.twbraam.jobwebsite_scraper.websites.Website

import scala.collection.immutable.ListMap

object PageSupervisor {
  sealed trait ScrapePageMessage
  final case class ScrapePageRequest(url: URL, replyTo: ActorRef[ScrapePageResponse]) extends ScrapePageMessage
  final case class ScrapePageResponse(scrapeResults: Map[String, Int]) extends ScrapePageMessage

  import KeywordScraper._

  def init(website: Website, replyTo: ActorRef[ScrapePageResponse]): Behavior[ScrapeJobResponse] =
    createScrapers(website, replyTo)

  private def createScrapers(website: Website, replyTo: ActorRef[ScrapePageResponse]): Behavior[ScrapeJobResponse] =
    Behaviors.setup { context =>
      val jobPageLinks: Set[URL] = website.extractLinks(website.url)
      val jobPageNum: Int = jobPageLinks.size

      val kwScraper = Behaviors.supervise(KeywordScraper.init(website)).onFailure[Exception](SupervisorStrategy.restart)
      val pool = Routers.pool(poolSize = 4)(kwScraper)
      val router = context.spawn(pool, "worker-pool")

      jobPageLinks.foreach { url =>
        router ! ScrapeJobRequest(url, context.self.ref)
      }

      awaitResults(website, Map.empty, jobPageNum, replyTo)
    }


  private def awaitResults(website: Website, acc: Map[String, Int], numLeft: Int,
                           replyTo: ActorRef[ScrapePageResponse]): Behavior[ScrapeJobResponse] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapeJobResponse(response) =>
          context.log.info(s"Got result: $response")

          val newAcc = response.foldLeft(acc)((acc, n) => {
            if (acc.isDefinedAt(n)) acc.updated(n, acc(n) + 1)
            else acc.updated(n, 1)
          })

          context.log.info(s"Current acc: ${ListMap(newAcc.toSeq.sortWith(_._2 > _._2):_*)}")
          if (numLeft <= 1) {
            replyTo ! ScrapePageResponse(acc)
            Behaviors.stopped
          }
          else awaitResults(website, newAcc, numLeft - 1, replyTo)
      }
    }
}
