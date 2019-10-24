package org.twbraam.jobwebsite_scraper.websites

import java.net.URL

import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model._

object FunctionalWorks extends Website {

  val url: URL = new URL("https://functional.works-hub.com/jobs/?tags=scala")

  override def toString: String = "Functional-Works"

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

  def extractJobLinks(pageUrl: URL): Set[URL] = {
    val browser: Browser = JsoupBrowser()
    val doc: browser.DocumentType = browser.get(pageUrl.toString)

    val linkElements: Seq[Element] = doc >> elementList(".button--inverted")

    linkElements.map(_ >> attr("href")("a"))
      .map(link => new URL("https://" + pageUrl.getHost + link))
      .toSet
  }

  def extractPageLinks: Set[URL] = {
    val browser = JsoupBrowser()
    val doc = browser.get(url.toString)

    val pageLinks: Iterable[Element#ThisType] = (doc >> elementList(".pagination-list")).head.children

    pageLinks.map(_ >> attr("href")("a"))
      .map(link => new URL("https://" + url.getHost + link))
      .toSet
  }
}
