package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.typed.scaladsl._
import akka.actor.typed._
import org.twbraam.jobwebsite_scraper.websites.Website

object ScraperSupervisor {
  import KeywordScraper._

  sealed trait ScraperSupervisorMessage

  final case class ScrapePageRequest(url: URL) extends ScraperSupervisorMessage

  case class ScrapePageResponse(scrapeResults: Map[String, Int]) extends ScraperSupervisorMessage

  final case class ScrapeJobResponseWrapper(msg: ScrapeJobResponse) extends ScraperSupervisorMessage


  def init(website: Website,
           replyTo: ActorRef[ScrapePageResponse]): Behavior[ScraperSupervisorMessage] = {
    Behaviors.setup { context =>
      context.log.info(s"Supervisor $website initialized")

      val kwScraper = Behaviors.supervise(KeywordScraper.init(website))
        .onFailure[Exception](SupervisorStrategy.restart)
      val pool = Routers.pool(poolSize = 4)(kwScraper)
      val router: ActorRef[KeywordScraperMessage] = context.spawnAnonymous(pool)
      awaitFirstMessage(website, replyTo: ActorRef[ScrapePageResponse], router)
    }
  }

  private def awaitFirstMessage(website: Website, replyTo: ActorRef[ScrapePageResponse],
                                scraperPool: ActorRef[KeywordScraperMessage]): Behavior[ScraperSupervisorMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapePageRequest(url) =>
          val jobPageLinks: Set[URL] = website.extractJobLinks(url)
          val jobAmount: Int = jobPageLinks.size
          val refWrapper = context.messageAdapter(ScrapeJobResponseWrapper)
          jobPageLinks.foreach { url => scraperPool ! ScrapeJobRequest(url, refWrapper) }

          awaitMessage(website, Map.empty, jobAmount, replyTo, scraperPool)
        case unexpected =>
          context.log.info(s"Unexpected message found: $unexpected, shutting down")
          Behaviors.stopped
        }
      }

  private def awaitMessage(website: Website, acc: Map[String, Int], numLeft: Int,
                           replyTo: ActorRef[ScrapePageResponse],
                           scraperPool: ActorRef[KeywordScraperMessage]): Behavior[ScraperSupervisorMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapePageRequest(url) =>
          context.log.info(s"Received request")
          val jobPageLinks: Set[URL] = website.extractJobLinks(url)
          val jobAmount: Int = jobPageLinks.size
          val refWrapper = context.messageAdapter(ScrapeJobResponseWrapper)
          jobPageLinks.foreach { url => scraperPool ! ScrapeJobRequest(url, refWrapper) }

          awaitMessage(website, acc, jobAmount + numLeft, replyTo, scraperPool)

        case ScrapeJobResponseWrapper(ScrapeJobResponse(response)) =>
          context.log.info(s"Received scraping result: $response")
          val newAcc = response.foldLeft(acc)((acc, n) => {
            if (acc.isDefinedAt(n)) acc.updated(n, acc(n) + 1)
            else acc.updated(n, 1)
          })

          if (numLeft > 1) awaitMessage(website, newAcc, numLeft - 1, replyTo, scraperPool)
          else {
            context.log.info(s"ScrapeSupervisor shutting down")
            replyTo ! ScrapePageResponse(acc)
            Behaviors.same
          }

        case unexpected =>
          context.log.info(s"Unexpected message found: $unexpected, shutting down")
          Behaviors.stopped
      }
    }
}
