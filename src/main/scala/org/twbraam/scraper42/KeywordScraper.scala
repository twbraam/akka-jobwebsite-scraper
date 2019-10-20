package org.twbraam.scraper42

import java.net.URL

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import org.twbraam.scraper42.websites.{FunctionalWorks, Website}

object KeywordScraper {
  sealed trait ScraperMessage
  final case class ScrapePageRequest(url: URL, replyTo: ActorRef[ScrapePageResponse]) extends ScraperMessage
  final case class ScrapePageResponse(scrapeResults: Set[String]) extends ScraperMessage

  def init(website: Website): Behavior[ScrapePageRequest] = scraper(website)

  private def scraper(website: Website): Behavior[ScrapePageRequest] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapePageRequest(url, replyTo) =>
          context.log.info(s"Scraping page: $url")

          val resp = website.parsePage(url)
          context.log.info(s"Response ${resp.mkString(", ")} sent to: $replyTo, shutting down...")
          replyTo ! ScrapePageResponse(resp)
          Behaviors.same
      }
    }
}
