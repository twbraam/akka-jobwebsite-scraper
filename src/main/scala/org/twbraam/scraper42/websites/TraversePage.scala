package org.twbraam.scraper42.websites

import java.net.URL

import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL.Parse._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model._
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}

object TraversePage {
  def extractLinks(pageUrl: URL): Set[URL] = {
    val browser: Browser = JsoupBrowser()
    val doc: browser.DocumentType = browser.get(pageUrl.toString)

    val linkElements: Seq[Element] = doc >> elementList(".button--inverted")

    linkElements.map(_ >> attr("href")("a"))
      .map(link => new URL("https://" + pageUrl.getHost + link))
      .toSet
  }
}
