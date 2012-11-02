package models

import anorm._
import java.util.Date
import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._

sealed case class GameStopScraper(pageN: Int = 1) extends Scraper with SafeMoney { 

  def getAll: List[GwithP] = scrapePage(pageN, allVals)
  def getGames: List[Game] = scrapePage(pageN, gameVals)
  def getPrices: List[Price] = scrapePage(pageN, priceVals)

  def scrapePage[A](pageN: Int, f: (Element) => A): List[A] = { 
    val doc = Jsoup.connect(GameStop.storeHead + ((pageN-1)*50).toString + GameStop.storeTail)
      .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
      .get()
    val searchResults = doc.getElementsByClass("product").toList

    searchResults.map(f(_))
  }

  def gameVals(html: Element): Game = {
    val name = html.getElementsByClass("product_info").select("a").text
    val gameUrl = "http://www.gamestop.com" +
    html.getElementsByClass("product_info").select("h3").attr("href")
    val imgUrl = "http://www.gamestop.com" + 
    html.getElementsByClass("grid_2").select("img").attr("src")

    Game(NotAssigned, name, GameStop.name, gameUrl, imgUrl)
  }

  def priceVals(html: Element): Price = {
    val name = html.getElementsByClass("product_info").select("a").text
    val priceS = $anitizer(html.getElementsByClass("pricing").text)
    val onSale = !html.getElementsByClass("old_price").text.isEmpty

    Price(NotAssigned, priceS, onSale, new Date(), 0)
  }

}

object GameStop extends StoreDetails { 

  val name = "GameStop"

  val storeHead = "http://www.gamestop.com/browse/pc?nav=2b"
  val storeTail = ",138c-ffff2418"

  // Fix this
  val finalPage = { 
    val doc = Jsoup.connect(storeHead + "1").get()
    val navNumStr = doc.getElementsByClass("pagination_controls").select("strong").text
    val navNumSep = navNumStr.split(' ').toList

    //navNumSep map { _.toInt } max
    43
  }
}
