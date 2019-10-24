package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import org.twbraam.jobwebsite_scraper.websites.Website

object KeywordScraper {
  sealed trait KeywordScraperMessage
  final case class ScrapeJobRequest(url: URL, replyTo: ActorRef[ScrapeJobResponse]) extends KeywordScraperMessage
  case class ScrapeJobResponse(scrapeResults: Set[String]) extends KeywordScraperMessage

  def init(website: Website): Behavior[KeywordScraperMessage] = scraper(website)

  private def scraper(website: Website): Behavior[KeywordScraperMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapeJobRequest(url, replyTo) =>
          context.log.info(s"Scraping page: $url")

          val resp = website.parsePage(url)
          replyTo ! ScrapeJobResponse(resp)
          Behaviors.same
      }
    }
}
