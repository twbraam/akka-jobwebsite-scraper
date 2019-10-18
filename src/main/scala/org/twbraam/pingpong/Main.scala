package org.twbraam.pingpong

import akka.actor.typed.ActorSystem

object Main {
  def main(args: Array[String]): Unit = {
    ActorSystem[Nothing](PingPongActor(), "ping-pong-system")
  }
}