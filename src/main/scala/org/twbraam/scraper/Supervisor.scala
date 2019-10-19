package org.twbraam.scraper

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.AbstractBehavior
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.Behaviors
import java.net.URL
import collection.mutable

object Supervisor {
  def apply(urls: List[URL]): Behavior[SupervisorMessage] =
    Behaviors.setup(context => new Supervisor(context, urls))

  trait SupervisorMessage

  final case class ScrapeSiteRequest(url: URL) extends SupervisorMessage
  final case class ScrapeSiteResponse(url: URL, response: Map[String, Int]) extends SupervisorMessage
}

class Supervisor(context: ActorContext[Supervisor.SupervisorMessage], urls: List[URL])
  extends AbstractBehavior[Supervisor.SupervisorMessage] {

  import Supervisor._
  import Manager._

  context.log.info("Job Website Scraper Application started")

  var childManagers: Map[URL, ActorRef[ManagerMessage]] =
    urls.map(url => (url, context.spawn(Manager(url, context.self.ref), "manager"))).toMap
  val allResponses: mutable.Map[String, Int] = mutable.Map()

  override def onMessage(msg: SupervisorMessage): Behavior[SupervisorMessage] = msg match {
    case ScrapeSiteResponse(url, response) =>
      response.foreach { case (name: String, n: Int) =>
        if (allResponses.isDefinedAt(name)) allResponses(name) += n
        else allResponses += (name -> n)
      }
      childManagers -= url
      outputWhenFinished()
  }

  override def onSignal: PartialFunction[Signal, Behavior[SupervisorMessage]] = {
    case PostStop =>
      context.log.info("Job Website Scraper Application stopped")
      this
  }

  private def outputWhenFinished(): Behavior[SupervisorMessage] = {
    if (childManagers.isEmpty) {
      allResponses.foreach(println)
      Behaviors.stopped
    } else {
      this
    }
  }
}