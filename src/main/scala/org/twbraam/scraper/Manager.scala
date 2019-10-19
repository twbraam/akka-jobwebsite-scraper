package org.twbraam.scraper

import java.net.URL

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import org.twbraam.scraper.Scraper.ScrapeResponse
import org.twbraam.scraper.Supervisor._

object Manager {
  def apply(url: URL, supervisor: ActorRef[SupervisorMessage]): Behavior[ManagerMessage] =
    Behaviors.setup(context => new Manager(context, url, supervisor: ActorRef[SupervisorMessage]))

  trait ManagerMessage extends SupervisorMessage
  final case class ScrapePageRequest(url: URL) extends ManagerMessage
  final case class ScrapePageResponse(url: URL, response: Map[String, Int]) extends ManagerMessage
}

class Manager(context: ActorContext[Manager.ManagerMessage], url: URL, supervisor: ActorRef[SupervisorMessage])
  extends AbstractBehavior[Manager.ManagerMessage] {
  import Manager._

  context.log.info(s"Manager $url started")

  def consolidate(): Map[String, Int] = ???

  val childScrapers: Iterable[ActorRef[Scraper.ScraperMessage]] =
    List(url).map(_ => context.spawn(Scraper(), "scraper"))

  override def onMessage(msg: ManagerMessage): Behavior[ManagerMessage] = msg match {
      case ScrapeResponse(url, response) =>
        supervisor ! ScrapePageResponse(url, response)
        this
    }

  override def onSignal: PartialFunction[Signal, Behavior[ManagerMessage]] = {
    case PostStop =>
      context.log.info(s"Manager $url stopped")
      this
  }
}