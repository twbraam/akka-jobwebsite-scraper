package org.twbraam.jobwebsite_scraper

import java.net.URL

import akka.actor.typed._
import akka.actor.typed.scaladsl._
import org.twbraam.jobwebsite_scraper.websites.Website

import scala.collection.immutable.ListMap

object Secretary {
  sealed trait SecretaryMessage
  final case class SecretaryRequest(website: Website) extends SecretaryMessage
  final case class SecretaryResponse(scrapeResults: Map[String, Int]) extends SecretaryMessage
  import SupervisorGroup._

  def init(replyTo: ActorRef[SecretaryResponse]): Behavior[SupervisorGroupMessage] =
    secretary(Set(), Map.empty, replyTo)

  private def secretary(openRequests: Set[Website], acc: Map[String, Int],
                        replyTo: ActorRef[SecretaryResponse]): Behavior[SupervisorGroupMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapeWebsiteResponse(website, scrapeResults) =>
          context.log.info(s"Secretary received results for website: $website")
          //context.spawn(SupervisorGroup.init(website, context.self.ref), s"supervisorGroup-$website")

          val newAcc = scrapeResults.foldLeft(acc) { case (acc, (kw, value)) =>
            if (acc.isDefinedAt(kw)) acc.updated(kw, acc(kw) + value)
            else acc.updated(kw, value)
          }

          if (openRequests.size > 1) secretary(openRequests - website, newAcc, replyTo)
          else {
            replyTo ! SecretaryResponse(acc)
            Behaviors.stopped
          }
        case unexpected => {
          context.log.info(s"Unexpected message found: $unexpected, shutting down")
          Behaviors.stopped
        }
      }
    }
}
