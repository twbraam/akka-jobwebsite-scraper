package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.typed._
import akka.actor.typed.scaladsl._
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
      val pageAmount: Int = pageLinks.size

      val refWrapper = context.messageAdapter(ScrapePageResponseWrapper)
      val scraperSupervisor = Behaviors.supervise(ScraperSupervisor.init(website, refWrapper)).onFailure[Exception](SupervisorStrategy.restart)

      val num_supervisors = 4
      val pool = Routers.pool(poolSize = num_supervisors)(scraperSupervisor)
      val supervisorPool = context.spawn(pool, s"supervisor-pool-$website")

      pageLinks.foreach { url => supervisorPool ! ScrapePageRequest(url) }

      awaitResults(website, Map.empty, pageAmount, replyTo)
    }


  private def awaitResults(website: Website, acc: Map[String, Int], numLeft: Int,
                           replyTo: ActorRef[ScrapeWebsiteResponse]): Behavior[SupervisorGroupMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapePageResponseWrapper(ScrapePageResponse(response)) =>
          context.log.info(s"Got page result: $response, still waiting on: $numLeft")

          val newAcc = response.foldLeft(acc) { case (acc, (kw, value)) =>
            if (acc.isDefinedAt(kw)) acc.updated(kw, acc(kw) + value)
            else acc.updated(kw, value)
          }

          if (numLeft > 1) awaitResults(website, newAcc, numLeft - 1, replyTo)
          else {
            replyTo ! ScrapeWebsiteResponse(website, newAcc)
            Behaviors.stopped
          }

        case unexpected =>
          context.log.info(s"Unexpected message found: $unexpected, shutting down")
          Behaviors.stopped
      }
    }
}
