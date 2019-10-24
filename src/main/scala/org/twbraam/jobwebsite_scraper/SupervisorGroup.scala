package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import org.twbraam.jobwebsite_scraper.ScraperSupervisor.ScrapePageResponse
import org.twbraam.jobwebsite_scraper.websites.Website

object SupervisorGroup {
  import ScraperSupervisor._

  sealed trait SupervisorGroupMessage
  final case class ScrapeWebsiteRequest(website: Website, replyTo: ActorRef[ScrapeWebsiteResponse]) extends SupervisorGroupMessage
  case class ScrapeWebsiteResponse(website: Website, scrapeResults: Map[String, Int]) extends SupervisorGroupMessage

  final case class ScrapePageResponseWrapper(msg: ScrapePageResponse) extends SupervisorGroupMessage

  def init(website: Website, replyTo: ActorRef[ScrapeWebsiteResponse]): Behavior[SupervisorGroupMessage] =
    createSupervisors(website, replyTo)

  private def createSupervisors(website: Website, replyTo: ActorRef[ScrapeWebsiteResponse]): Behavior[SupervisorGroupMessage] =
    Behaviors.setup { context =>
      val pageLinks: Set[URL] = website.extractPageLinks

      val refWrapper = context.messageAdapter(ScrapePageResponseWrapper)
      pageLinks.zipWithIndex.map { case (url, n) =>
        context.spawn(ScraperSupervisor.init(url, website, refWrapper, n), s"supervisor-$n")
      }

      val children = (0 until pageLinks.size).toList
      awaitResults(website, Map.empty, children, replyTo)
    }


  private def awaitResults(website: Website, acc: Map[String, Int], children: List[Int],
                           replyTo: ActorRef[ScrapeWebsiteResponse]): Behavior[SupervisorGroupMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapePageResponseWrapper(ScrapePageResponse(response, id)) =>
          val newChildren = children.filterNot(_ == id)
          context.log.info(s"Got page $id result: $response, still waiting on: $newChildren")

          val newAcc = response.foldLeft(acc) { case (acc, (kw, value)) =>
            if (acc.isDefinedAt(kw)) acc.updated(kw, acc(kw) + value)
            else acc.updated(kw, value)
          }

          if (newChildren.nonEmpty) awaitResults(website, newAcc, newChildren, replyTo)
          else {
            replyTo ! ScrapeWebsiteResponse(website, acc)
            Behaviors.stopped
          }

        case unexpected =>
          context.log.info(s"Unexpected message found: $unexpected, shutting down")
          Behaviors.stopped
      }
    }
}
