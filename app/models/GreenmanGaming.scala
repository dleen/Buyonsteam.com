package models

import anorm._
import java.util.Date
import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._
import scala.util.control.Exception._
import org.postgresql.util._

sealed case class GreenmanGamingScraper(pageN: Int = 1) extends Scraper with SafeMoney {

  def getAll: List[GwithP] = scrapePage(pageN, allVals)
  def getGames: List[Game] = scrapePage(pageN, gameVals)
  def getPrices: List[Price] = scrapePage(pageN, priceVals)

  def scrapePage[A](pageN: Int, f: (Element) => A): List[A] = {
    try {
      val doc = Jsoup.connect(GreenmanGamingScraper.storeHead + pageN.toString)
        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
        .timeout(30000)
        .get()
      val searchResults = doc.select("li.border-container").toList
      searchResults.map(f(_))
    } catch {
      case e =>
        println(e)
        List()
    }
  }

  def gameVals(html: Element): Game = {
    val name = html.select("h2").text
    val gameUrl = "http://www.greenmangaming.com" + html.select("a").attr("href")
    val imgUrl = html.select("img.cover").attr("src")

    Game(NotAssigned, name, GreenmanGamingScraper.name, gameUrl, imgUrl)
  }

  def priceVals(html: Element): Price = {
    val name = html.select("h2").text
    val priceS = $anitizer(html.select("strong.curPrice").text)
    val onSale = !html.select("span.lt").isEmpty

    Price(NotAssigned, priceS, onSale, new Date(), 0)
  }

}

object GreenmanGamingScraper {

  val name = "GreenmanGaming"

  val storeHead = "http://www.greenmangaming.com/s/us/en/pc/games/?page="

  val finalPage = {
    val doc = Jsoup.connect(storeHead + "1").get()
    val navNumStr = doc.getElementsByClass("paginator").select("a").text
    val navNumSep = navNumStr.split(' ').toList

    navNumSep(2).toInt
  }

  def reindex = {
    for (i <- (1 to finalPage).par) {
      val G = GreenmanGamingScraper(i).getAll
      G flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertGame(x))
      G flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertPrice(x))
    }
  }

}
