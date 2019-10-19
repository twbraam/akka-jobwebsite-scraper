package org.twbraam.scraper

import akka.actor.typed.ActorSystem
import java.net.URL

object Main {
  def main(args: Array[String]): Unit = {
    val urls = List[URL](new URL("https://functional.works-hub.com/"))

    ActorSystem[Nothing](Manager(urls), "scraper-system")
  }
}