package models

import anorm._
import java.util.Date
import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._
import scala.util.control.Exception._
import org.postgresql.util._

case class GameStopScraper(pageN: Int = 1) extends Scraper with SafeMoney {

  def getAll: List[GwithP] = scrapePage(pageN, allVals)
  def getGames: List[Game] = scrapePage(pageN, gameVals)
  def getPrices: List[Price] = scrapePage(pageN, priceVals)

  def scrapePage[A](pageN: Int, f: (Element) => A): List[A] = {
    try {
      val doc = Jsoup.connect(GameStopScraper.storeHead + ((pageN - 1) * 12).toString + GameStopScraper.storeTail)
        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
        .timeout(15000)
        .get()
      val searchResults = doc.getElementsByClass("product").toList
      searchResults.map(f(_))
    } catch {
      case e =>
        println(e)
        List()
    }

  }

  def gameVals(html: Element): Game = {
    val name = html.getElementsByClass("product_info").select("h3").select("a").text
    val gameUrl = "http://www.gamestop.com" +
      html.getElementsByClass("product_info").select("h3").select("a").attr("href")
    val imgUrl = "http://www.gamestop.com" +
      html.getElementsByClass("grid_2").select("img").attr("src")

    Game(NotAssigned, name, GameStopScraper.name, gameUrl, imgUrl)
  }

  def priceVals(html: Element): Price = {
    val name = html.getElementsByClass("product_info").select("a").text
    val priceS = $anitizer(html.getElementsByClass("pricing").text)
    val onSale = !html.getElementsByClass("old_price").text.isEmpty

    Price(NotAssigned, priceS, onSale, new Date(), 0)
  }

}

object GameStopScraper {

  val name = "GameStop"

  val storeHead = "http://www.gamestop.com/browse/pc?nav=2b"
  val storeTail = ",138c-ffff2418"

  val finalPage = {
    val doc = Jsoup.connect(storeHead + storeTail).get()
    val navNumStr = doc.getElementsByClass("pagination_controls").select("strong").text
    val navNumSep = navNumStr.split(' ').toList.last.drop(1)

    navNumSep.toInt
  }

  def reindex = {
    for (i <- (1 to finalPage).par) {
      val G = GameStopScraper(i).getAll
      G flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertGame(x))
      G flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertPrice(x))
    }
  }

}
