package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import org.twbraam.jobwebsite_scraper.websites.Website

object KeywordScraper {
  sealed trait ScrapeJobMessage
  final case class ScrapeJobRequest(url: URL, replyTo: ActorRef[ScrapeJobResponse]) extends ScrapeJobMessage
  final case class ScrapeJobResponse(scrapeResults: Set[String]) extends ScrapeJobMessage

  def init(website: Website): Behavior[ScrapeJobRequest] = scraper(website)

  private def scraper(website: Website): Behavior[ScrapeJobRequest] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapeJobRequest(url, replyTo) =>
          context.log.info(s"Scraping page: $url")

          val resp = website.parsePage(url)
          context.log.info(s"Response ${resp.mkString(", ")} sent to: $replyTo, shutting down...")
          replyTo ! ScrapeJobResponse(resp)
          Behaviors.same
      }
    }
}
