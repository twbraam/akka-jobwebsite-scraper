package org.twbraam.scraper42

import java.net.URL

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import org.twbraam.scraper42.websites.{FunctionalWorks, Website}

object Scraper {
  sealed trait ScraperMessage
  final case class ScrapePageRequest(url: URL, replyTo: ActorRef[ScraperMessage]) extends ScraperMessage
  private final case class ScrapePageResponse(scrapeResults: Map[String, Int]) extends ScraperMessage


  def init(website: Website): Behavior[ScraperMessage] = scraper(website)

  private def scraper(website: Website): Behavior[ScraperMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapePageRequest(url, replyTo) =>
          context.log.info(s"Scraping page: $url")

          val resp = website.parsePage(url)
          replyTo ! ScrapePageResponse(resp)
          context.log.info(s"Response ${resp.mkString(", ")} sent to: $replyTo, shutting down...")
          Behaviors.stopped
      }
    }
}
