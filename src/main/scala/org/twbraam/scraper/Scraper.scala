package org.twbraam.scraper

import java.net.URL

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import org.twbraam.scraper.Manager.ManagerMessage

object Scraper {
  def apply(): Behavior[ScraperMessage] =
    Behaviors.setup(context => new Scraper(context))

  sealed trait ScraperMessage
  final case class ScrapeRequest(url: URL, replyTo: ActorRef[ScrapeResponse]) extends ScraperMessage
  final case class ScrapeResponse(url: URL, response: Map[String, Int]) extends ManagerMessage
}

class Scraper(context: ActorContext[Scraper.ScraperMessage])
  extends AbstractBehavior[Scraper.ScraperMessage] {
  import Scraper._

  def scrape(url: URL): Map[String, Int] = ???

  context.log.info("Scraper actor started")
  var urlsParsed: List[URL] = List()

  override def onMessage(msg: ScraperMessage): Behavior[ScraperMessage] = msg match {
      case ScrapeRequest(url: URL, replyTo) =>
        replyTo ! ScrapeResponse(url, scrape(url))
        urlsParsed = url :: urlsParsed
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[ScraperMessage]] = {
    case PostStop =>
      context.log.info("Scraper actor stopped")
      this
  }
}