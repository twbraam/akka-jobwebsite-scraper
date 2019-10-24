package org.twbraam.jobwebsite_scraper.websites

import java.net.URL

import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model._

object Archer extends Website {

  val url: URL = new URL("https://www.archer.eu/scala-jobs/")

  override def toString: String = "Archer"

  def parsePage(jobUrl: URL): Set[String] = {
    val browser = JsoupBrowser()
    val doc = browser.get(jobUrl.toString)

    val description = doc >> text(".job_description")
    description
      .replaceAll("[^a-zA-Z ]", " ")
      .split(" ")
      .filter(_.nonEmpty)
      .filter(word => word.head.isUpper)
      .toSet
  }

  def extractJobLinks(pageUrl: URL): Set[URL] = {
    val browser: Browser = JsoupBrowser()
    val doc: browser.DocumentType = browser.get(pageUrl.toString)

    (doc >> elementList(".job_listing"))
      .map(ele => ele("a") >> attr("href"))
      .map(new URL(_))
      .toSet
  }

  def extractPageLinks: Set[URL] =
    Set(url)
}
