package org.twbraam.pingpong

import akka.actor.typed.ActorRef

object Messages {
  sealed trait Message
  final case class Ping(id: Long, replyTo: ActorRef[Pong]) extends Message
  final case class Pong(id: Long, value: Option[String])
}