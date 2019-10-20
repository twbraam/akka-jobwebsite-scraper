package org.twbraam.scraper42.websites

import java.net.URL
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

object FunctionalWorks extends Website {
  val url: URL = new URL("https://functional.works-hub.com/scala-jobs")
  def parsePage(pageUrl: URL): Map[String, Int] = {
    val doc = JsoupBrowser()
      .get(url.toString)
    val jobDescr = doc >> text(".job__job-description")

  }
}
