package org.twbraam.jobwebsite_scraper.websites

import java.net.URL

trait Website {
  val url: URL
  def parsePage(pageUrl: URL): Set[String]
  def extractLinks(pageUrl: URL): Set[URL]
}