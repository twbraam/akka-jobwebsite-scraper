package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import org.twbraam.jobwebsite_scraper.websites.Website

object ScraperSupervisor {
  import KeywordScraper._

  sealed trait ScraperSupervisorMessage
  case class ScrapePageResponse(scrapeResults: Map[String, Int], id: Int) extends ScraperSupervisorMessage

  final case class ScrapeJobResponseWrapper(msg: ScrapeJobResponse) extends ScraperSupervisorMessage

  def init(pageURL: URL, website: Website,
           replyTo: ActorRef[ScrapePageResponse], id: Int): Behavior[ScraperSupervisorMessage] =
    createScrapers(pageURL, website, replyTo, id)

  private def createScrapers(pageURL: URL, website: Website, replyTo: ActorRef[ScrapePageResponse],
                             id: Int): Behavior[ScraperSupervisorMessage] =
    Behaviors.setup { context =>
      val jobPageLinks: Set[URL] = website.extractJobLinks(pageURL)
      val jobPageNum: Int = jobPageLinks.size

      val kwScraper = Behaviors.supervise(KeywordScraper.init(website)).onFailure[Exception](SupervisorStrategy.restart)
      val pool = Routers.pool(poolSize = 4)(kwScraper)
      val router = context.spawn(pool, s"keywordScraper-pool-$id")

      val refWrapper = context.messageAdapter(ScrapeJobResponseWrapper)
      jobPageLinks.foreach { url =>
        router ! ScrapeJobRequest(url, refWrapper)
      }

      awaitResults(website, Map.empty, jobPageNum, replyTo, id)
    }


  private def awaitResults(website: Website, acc: Map[String, Int], numLeft: Int,
                           replyTo: ActorRef[ScrapePageResponse], id: Int): Behavior[ScraperSupervisorMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapeJobResponseWrapper(ScrapeJobResponse(response)) =>

          context.log.info(s"Got job result: $response")

          val newAcc = response.foldLeft(acc)((acc, n) => {
            if (acc.isDefinedAt(n)) acc.updated(n, acc(n) + 1)
            else acc.updated(n, 1)
          })

          if (numLeft > 1) awaitResults(website, newAcc, numLeft - 1, replyTo, id)
          else {
            context.log.info(s"ScrapeSupervisor $id Shutting down")
            replyTo ! ScrapePageResponse(acc, id)
            Behaviors.stopped
          }

        case unexpected =>
          context.log.info(s"Unexpected message found: $unexpected, shutting down")
          Behaviors.stopped
      }
    }
}
