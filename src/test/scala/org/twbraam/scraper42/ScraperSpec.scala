package org.twbraam.scraper42

import java.net.URL

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed._
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import org.scalatest.WordSpecLike
import org.twbraam.scraper42.Scraper.{ScrapePageRequest, ScraperMessage}
import org.twbraam.scraper42.websites.FunctionalWorks

class ScraperSpec extends ScalaTestWithActorTestKit with WordSpecLike {

  "Hello world" must {
    "be simple" in {
      val main: Behavior[ScraperMessage] =
        Behaviors.setup { context: ActorContext[ScraperMessage] =>
          val worker = context.spawn(Scraper.init(FunctionalWorks), "simple")

          worker ! ScrapePageRequest(new URL("https://functional.works-hub.com/jobs/618"), context.self.ref)

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