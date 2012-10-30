package models

import anorm._
import java.util.Date
import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._

case class SteamScraper(pageN: Int = 1) extends Scraper with SafeMoney {

  def getAll: List[Combined] = scrapePage(pageN, allVals)
  def getGames: List[Game] = scrapePage(pageN, gameVals)
  def getPrices: List[Price] = scrapePage(pageN, priceVals)

  def scrapePage[A](pageN: Int, f: (Element) => A): List[A] = {
    val doc = Jsoup.connect(SteamDets.storeHead + pageN.toString)
      .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
      .get()
    val searchResults = doc.getElementsByClass("search_result_row").toList

    searchResults.map(f(_))
  }

  def gameVals(html: Element): Game = {
    val name = html.select("h4").text
    val gameUrl = html.select("a").attr("href")
    val gameId = SteamScraper.appId(gameUrl)
    val imgUrl = html.getElementsByClass("search_capsule").select("img").attr("src")
    val releaseDate = html.getElementsByClass("search_released").text
    val meta = SteamScraper.scorefS(html.getElementsByClass("search_metascore").text)

    Game(NotAssigned, gameId, name, gameUrl, imgUrl, releaseDate, meta)
  }

  def priceVals(html: Element): Price = {
    val name = html.select("h4").text
    val priceS = $anitizer(html.getElementsByClass("search_price").text)
    val onSale = !html.select("strike").isEmpty

    Price(NotAssigned, name, priceS, None, new Date(), onSale, None)
  }

}

object SteamScraper {

  def appId(url: String): Option[Int] = {
    if (url.length < 42) None
    else if (url(30) == 'v') None
    else Some(url.substring(34, url.indexOf('/', 34)).toInt)
  }

  def scorefS(meta: String): Option[Int] = {
    if (meta.isEmpty) None
    else Some(meta.toInt)
  }

}

object SteamDets extends StoreDetails {

  val storeHead =
    "http://store.steampowered.com/search/results?&cc=us&category1=998&page="

  val finalPage = {
    val doc = Jsoup.connect(storeHead + "1").get()
    val navNumStr = doc.getElementsByClass("search_pagination_right").select("a").text
    val navNumSep = navNumStr.split(' ')

    navNumSep(2).toInt
  }
}