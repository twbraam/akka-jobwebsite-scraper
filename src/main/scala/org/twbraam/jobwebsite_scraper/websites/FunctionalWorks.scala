package org.twbraam.jobwebsite_scraper.websites

import java.net.URL
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.browser.JsoupBrowser

object FunctionalWorks extends Website {
  val url: URL = new URL("https://functional.works-hub.com/jobs/?tags=scala")
  def parsePage(jobUrl: URL): Set[String] = {
    val browser = JsoupBrowser()
    val doc = browser.get(jobUrl.toString)

    val description = doc >> text(".job__job-description")
    description
      .replaceAll("[^a-zA-Z ]", " ")
      .split(" ")
      .filter(_.nonEmpty)
      .filter(word => word.head.isUpper)
      .toSet
  }
}
