package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import org.twbraam.jobwebsite_scraper.websites.Website

import scala.collection.immutable.ListMap

object ScraperSupervisor {
  import KeywordScraper._

  sealed trait ScrapePageMessage
  final case class ScrapePageRequest(url: URL, replyTo: ActorRef[ScrapePageResponse]) extends ScrapePageMessage
  final case class ScrapePageResponse(scrapeResults: Map[String, Int], supervisorId: ActorRef[ScrapeJobResponse]) extends ScrapePageMessage

  def init(pageURL: URL, website: Website, replyTo: ActorRef[ScrapePageResponse]): Behavior[ScrapeJobResponse] =
    createScrapers(pageURL, website, replyTo)

  private def createScrapers(pageURL: URL, website: Website, replyTo: ActorRef[ScrapePageResponse]): Behavior[ScrapeJobResponse] =
    Behaviors.setup { context =>
      val jobPageLinks: Set[URL] = website.extractJobLinks(pageURL)
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
          context.log.info(s"Got job result: $response")

          val newAcc = response.foldLeft(acc)((acc, n) => {
            if (acc.isDefinedAt(n)) acc.updated(n, acc(n) + 1)
            else acc.updated(n, 1)
          })

          if (numLeft > 1) awaitResults(website, newAcc, numLeft - 1, replyTo)
          else {
            replyTo ! ScrapePageResponse(acc, context.self.ref)
            Behaviors.stopped
          }
      }
    }
}
