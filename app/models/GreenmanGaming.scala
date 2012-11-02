package models

import anorm._
import java.util.Date
import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._

sealed case class GreenmanGamingScraper(pageN: Int = 1) extends Scraper with SafeMoney {

  def getAll: List[GwithP] = scrapePage(pageN, allVals)
  def getGames: List[Game] = scrapePage(pageN, gameVals)
  def getPrices: List[Price] = scrapePage(pageN, priceVals)

  def scrapePage[A](pageN: Int, f: (Element) => A): List[A] = { 
    val doc = Jsoup.connect(GreenmanGaming.storeHead + pageN.toString)
      .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
      .get()
    val searchResults = doc.getElementsByClass("border-container").toList

    searchResults.map(f(_))
  }

  def gameVals(html: Element): Game = {
    val name = html.attr("h2")
    val gameUrl = html.attr("href")
    val imgUrl = html.select("img").attr("src")

    Game(NotAssigned, name, GreenmanGaming.name, gameUrl, imgUrl)
  }

  def priceVals(html: Element): Price = {
    val name = html.attr("h2")
    val priceS = $anitizer(html.getElementsByClass("curPrice").text)
    val onSale = !html.select("lt").isEmpty

    Price(NotAssigned, priceS, onSale, new Date(), 0)
  }

}

object GreenmanGaming extends StoreDetails {

  val name = "GreenmanGaming"

  val storeHead =
    "http://www.greenmangaming.com/s/us/en/pc/games/?page="

  val finalPage = {
    val doc = Jsoup.connect(storeHead + "1").get()
    val navNumStr = doc.getElementsByClass("paginator").select("a").text
    val navNumSep = navNumStr.split(' ').toList

    navNumSep map { _.toInt } max
  }
}
