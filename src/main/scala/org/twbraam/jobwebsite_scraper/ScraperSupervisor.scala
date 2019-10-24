package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.typed.scaladsl._
import akka.actor.typed._
import org.twbraam.jobwebsite_scraper.websites.Website

object ScraperSupervisor {
  import KeywordScraper._

  sealed trait ScraperSupervisorMessage
  final case class ScrapePageRequest(url: URL, replyTo: ActorRef[ScrapePageResponse]) extends ScraperSupervisorMessage
  case class ScrapePageResponse(scrapeResults: Map[String, Int], id: Int) extends ScraperSupervisorMessage

  final case class ScrapeJobResponseWrapper(msg: ScrapeJobResponse) extends ScraperSupervisorMessage



  def init(website: Website, id: Int,
           replyTo: ActorRef[ScrapePageResponse]): Behavior[ScraperSupervisorMessage] = {
    Behaviors.setup { context =>
      val kwScraper = Behaviors.supervise(KeywordScraper.init(website))
        .onFailure[Exception](SupervisorStrategy.restart)
      val pool = Routers.pool(poolSize = 4)(kwScraper)
      val router: ActorRef[KeywordScraperMessage] = context.spawn(pool, s"keywordScraper-pool-$id")
      awaitFirstMessage(website, replyTo: ActorRef[ScrapePageResponse], router, id)
    }
  }

  private def awaitFirstMessage(website: Website, replyTo: ActorRef[ScrapePageResponse],
                                scraperPool: ActorRef[KeywordScraperMessage], id: Int): Behavior[ScraperSupervisorMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapePageRequest(url, replyTo) =>
          val jobPageLinks: Set[URL] = website.extractJobLinks(url)
          val jobPageNum: Int = jobPageLinks.size
          val refWrapper = context.messageAdapter(ScrapeJobResponseWrapper)
          jobPageLinks.foreach { url => scraperPool ! ScrapeJobRequest(url, refWrapper) }

          awaitMessage(website, Map.empty, jobPageNum, replyTo, id, scraperPool)
        }
      }

  private def awaitMessage(website: Website, acc: Map[String, Int], numLeft: Int,
                           replyTo: ActorRef[ScrapePageResponse], id: Int,
                           scraperPool: ActorRef[KeywordScraperMessage]): Behavior[ScraperSupervisorMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapePageRequest(url, replyTo) =>
          context.log.info(s"Received request")
          val jobPageLinks: Set[URL] = website.extractJobLinks(url)
          val jobPageNum: Int = jobPageLinks.size
          val refWrapper = context.messageAdapter(ScrapeJobResponseWrapper)
          jobPageLinks.foreach { url => scraperPool ! ScrapeJobRequest(url, refWrapper) }

          awaitMessage(website, acc, jobPageNum + numLeft, replyTo, id, scraperPool)

        case ScrapeJobResponseWrapper(ScrapeJobResponse(response)) =>
          context.log.info(s"Received scraping result: $response")
          val newAcc = response.foldLeft(acc)((acc, n) => {
            if (acc.isDefinedAt(n)) acc.updated(n, acc(n) + 1)
            else acc.updated(n, 1)
          })

          if (numLeft > 1) awaitMessage(website, newAcc, numLeft - 1, replyTo, id, scraperPool)
          else {
            context.log.info(s"ScrapeSupervisor $id Shutting down")
            replyTo ! ScrapePageResponse(acc, id)
            Behaviors.same
          }

        case unexpected =>
          context.log.info(s"Unexpected message found: $unexpected, shutting down")
          Behaviors.stopped
      }
    }
}
