package org.twbraam.scraper2

import akka.NotUsed
import akka.actor.typed._
import akka.actor.typed.scaladsl.Behaviors

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ChatRoom {
  sealed trait ManagerCommand
  final case class ScrapeWebsiteRequest(url: String, replyTo: ActorRef[SessionEvent]) extends ManagerCommand
  private final case class PublishSessionMessage(url: String, message: String) extends ManagerCommand

  sealed trait SessionEvent
  final case class SessionGranted(handle: ActorRef[ScrapePageRequest]) extends SessionEvent
  final case class SessionDenied(reason: String) extends SessionEvent
  final case class MessagePosted(url: String, message: String) extends SessionEvent

  trait ScraperCommand
  final case class ScrapePageRequest(message: String) extends ScraperCommand
  private final case class NotifyClient(message: MessagePosted) extends ScraperCommand

  val behavior: Behavior[ManagerCommand] =
    chatRoom(List.empty)

  private def chatRoom(sessions: List[ActorRef[ScraperCommand]]): Behavior[ManagerCommand] =
    Behaviors.receive { (context, message) =>
      message match {
        case ScrapeWebsiteRequest(url, client) =>
          // create a child actor for further interaction with the client
          val ses = context.spawn(
            scraper(context.self, url),
            name = URLEncoder.encode(url, StandardCharsets.UTF_8.name))
          client ! SessionGranted(ses)
          chatRoom(ses :: sessions)
        case PublishSessionMessage(url, message) =>
          val notification = NotifyClient(MessagePosted(url, message))
          sessions.foreach(_ ! notification)
          Behaviors.same
      }
    }

  private def scraper(manager: ActorRef[PublishSessionMessage],
                      url: String): Behavior[ScraperCommand] =
    Behaviors.receiveMessage {
      case ScrapePageRequest(message) =>
        // from client, publish to others via the manager
        manager ! PublishSessionMessage(url, message)
        Behaviors.same
    }
}