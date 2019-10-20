package org.twbraam.superSimple

import akka.NotUsed
import akka.actor.typed._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import org.scalatest.WordSpecLike

class SimpleSpec extends ScalaTestWithActorTestKit with WordSpecLike {
  import Simple._

  "Hello world" must {
    "be simple" in {
      val main: Behavior[NotUsed] =
        Behaviors.setup { context: ActorContext[NotUsed] =>
          val manager = context.spawn(Simple.behavior, "simple")

          manager ! CreateScraper()

          Behaviors.receiveSignal {
            case (_, Terminated(_)) =>
              Behaviors.stopped
          }
        }

      val system = ActorSystem(main, "SimpleDemo")
      system.whenTerminated // remove compiler warnings
    }
  }

}