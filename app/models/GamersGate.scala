package models

import anorm._
import java.util.Date
import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._

case class GamersGateScraper(pageN: Int = 1) extends Scraper with SafeMoney {

  def getAll: List[GwithP] = scrapePage(pageN, allVals)
  def getGames: List[Game] = scrapePage(pageN, gameVals)
  def getPrices: List[Price] = scrapePage(pageN, priceVals)

  def scrapePage[A](pageN: Int, f: (Element) => A): List[A] = {
    val doc = Jsoup.connect(GamersGateDets.storeHead + pageN.toString)
      .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
      .get()
    val searchResults = doc.getElementsByClass("product_display").toList

    searchResults.map(f(_))
  }

  def gameVals(html: Element): Game = {
    val name = html.getElementsByClass("descr_cont").select("a").attr("title")
    val gameUrl = html.getElementsByClass("descr_cont").select("a").attr("href")
    val imgUrl = html.getElementsByClass("box_cont").select("img").attr("src")

    Game(NotAssigned, name, GamersGateDets.name, gameUrl, imgUrl)
  }

  def priceVals(html: Element): Price = {
    val name = html.getElementsByClass("descr_cont").select("a").attr("title")
    val tempPrice = html.getElementsByClass("prtag").first.children
    val priceS = $anitizer(tempPrice.first.ownText)
    val onSale = !html.getElementsByClass("discount").isEmpty

    Price(NotAssigned, priceS, onSale, new Date(), 0)
  }

}

object GamersGateDets extends StoreDetails {

  val name = "GamersGate"
    
  val storeHead =
    "http://www.gamersgate.com/games?filter=available&pg="

  val finalPage = {
    val doc = Jsoup.connect(storeHead + "1").get()
    val navNumStr = doc.getElementsByClass("paginator").select("a").text
    val navNumSep = navNumStr.split(' ').toList

    navNumSep map { _.toInt } max
  }
}