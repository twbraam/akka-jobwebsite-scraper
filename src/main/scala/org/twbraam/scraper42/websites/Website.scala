package org.twbraam.scraper42.websites

import java.net.URL

trait Website {
  val url: URL
  def parsePage(pageUrl: URL): Set[String]
}