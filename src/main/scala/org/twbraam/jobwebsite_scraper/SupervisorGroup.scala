package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import org.twbraam.jobwebsite_scraper.websites.Website

import scala.collection.immutable.ListMap

object SupervisorGroup {
  sealed trait ScrapeWebsiteMessage
  final case class ScrapeWebsiteRequest(website: Website, replyTo: ActorRef[ScrapeWebsiteResponse]) extends ScrapeWebsiteMessage
  final case class ScrapeWebsiteResponse(scrapeResults: Map[String, Int]) extends ScrapeWebsiteMessage
  import ScraperSupervisor._
  import KeywordScraper._

  def init(website: Website, replyTo: ActorRef[ScrapeWebsiteResponse]): Behavior[ScrapePageResponse] =
    createSupervisors(website, replyTo)

  private def createSupervisors(website: Website, replyTo: ActorRef[ScrapeWebsiteResponse]): Behavior[ScrapePageResponse] =
    Behaviors.setup { context =>
      val pageLinks: Set[URL] = website.extractPageLinks

      val supervisors: Set[ActorRef[ScrapeJobResponse]] =
        pageLinks.zipWithIndex.map { case (url, n) =>
          context.spawn(ScraperSupervisor.init(url, website, context.self.ref), s"supervisor-$n")

        }

      awaitResults(website, Map.empty, supervisors, replyTo)
    }


  private def awaitResults(website: Website, acc: Map[String, Int], children: Set[ActorRef[ScrapeJobResponse]],
                           replyTo: ActorRef[ScrapeWebsiteResponse]): Behavior[ScrapePageResponse] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapePageResponse(response, id) =>
          context.log.info(s"Got page result: $response")

          val newAcc = response.foldLeft(acc) { case (acc, (kw, value)) =>
            if (acc.isDefinedAt(kw)) acc.updated(kw, acc(kw) + value)
            else acc.updated(kw, value)
          }

          if (children.size > 1) awaitResults(website, newAcc, children - id, replyTo)
          else {
            replyTo ! ScrapeWebsiteResponse(acc)
            Behaviors.stopped
          }
      }
    }
}
