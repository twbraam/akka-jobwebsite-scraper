package org.twbraam.superSimple

import akka.actor.typed._
import akka.actor.typed.scaladsl._

object Simple {
  sealed trait Message
  final case class CreateScraper() extends Message
  final case class MessageIn() extends Message
  private final case class MessageOut(message: String) extends Message


  val behavior: Behavior[Message] =
    first()

  private def first(): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case CreateScraper() =>
          val child = context.spawn(
            scraper(context.self, "Kak en poep"), "child")
          child ! MessageIn()
          Behaviors.same
        case MessageOut(message) =>
          context.log.info("Message received: " + message)
          Behaviors.same
      }
    }

  private def scraper(manager: ActorRef[MessageOut],
                      url: String): Behavior[Message] =
    Behaviors.receive { (context, message) =>
      message match {
        case MessageIn() =>
          context.log.info(s"Sending message to manager: $manager")
          manager ! MessageOut(url)
          context.log.info("Shutting down...")
          Behaviors.stopped
    }
  }
}
