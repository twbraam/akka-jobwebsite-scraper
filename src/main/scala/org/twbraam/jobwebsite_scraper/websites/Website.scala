package org.twbraam.jobwebsite_scraper.websites

import java.net.URL

trait Website {
  val url: URL
  def parsePage(pageUrl: URL): Set[String]
  def extractJobLinks(pageUrl: URL): Set[URL]
  def extractPageLinks: Set[URL]
}