package org.twbraam.scraper

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import java.net.URL

import org.twbraam.scraper.Scraper.{Passivate, ScrapeRequest, ScrapeResponse}
import org.twbraam.scraper.Supervisor.SupervisorMessage

object Manager {
  def apply(url: URL, replyTo: ActorRef[SupervisorMessage]): Behavior[ManagerMessage] =
    Behaviors.setup(context => new Manager(context, url, replyTo: ActorRef[SupervisorMessage]))

  trait ManagerMessage

  final case class ScrapePageRequest(requestId: Long, url: URL) extends ManagerMessage
  final case class ScrapePageResponse(requestId: Long, response: Map[String, Int]) extends ManagerMessage
}

class Manager(context: ActorContext[Manager.ManagerMessage], url: URL, replyTo: ActorRef[SupervisorMessage])
  extends AbstractBehavior[Manager.ManagerMessage] {
  import Manager._

  context.log.info(s"Manager $url started")

  def consolidate(): Map[String, Int] = ???

  val indexToURL: Map[Int, URL] = List(url).zipWithIndex.map {case (url, n) => (n, url)}.toMap
  val childScrapers: Iterable[ActorRef[Scraper.ScraperMessage]] =
    indexToURL.keys.map(scraperId => context.spawn(Scraper(scraperId), "scraper-" + scraperId))

  override def onMessage(msg: ManagerMessage): Behavior[ManagerMessage] = {
    msg match {
      case ScrapeResponse(requestId, url) =>
        replyTo ! ScrapeResponse(id, scrape(url))
        this

      case Passivate =>
        Behaviors.stopped
    }
  }

  override def onSignal: PartialFunction[Signal, Behavior[ManagerMessage]] = {
    case PostStop =>
      context.log.info(s"Manager $url stopped")
      this
  }
}