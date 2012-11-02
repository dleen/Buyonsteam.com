package models

import anorm._
import java.util.Date
import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._
import scala.util.control.Exception._
import org.postgresql.util._

case class DlGamerScraper(pageN: Int = 1) extends Scraper with SafeMoney {

  def getAll: List[GwithP] = scrapePage(pageN, allVals)
  def getGames: List[Game] = scrapePage(pageN, gameVals)
  def getPrices: List[Price] = scrapePage(pageN, priceVals)

  def scrapePage[A](pageN: Int, f: (Element) => A): List[A] = {
    try {
      val doc = Jsoup.connect(DlGamerScraper.storeHead + pageN.toString)
        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
        .get()
      val searchResults = doc.getElementsByClass("box").toList
      searchResults.map(f(_))
    } catch {
      case e =>
        println(e)
        List()
    }
  }

  def gameVals(html: Element): Game = {
    val name = html.getElementsByClass("title").text
    val gameUrl = html.getElementsByClass("title").select("a").attr("href")
    val imgUrl = "http://dlgamer.us" + html.select("img").attr("src")

    Game(NotAssigned, name, DlGamerScraper.name, gameUrl, imgUrl)
  }

  def priceVals(html: Element): Price = {
    val name = html.getElementsByClass("title").text
    val priceS = $anitizer(html.getElementsByClass("price").text)
    val onSale = !html.getElementsByClass("old-price").text.isEmpty

    Price(NotAssigned, priceS, onSale, new Date(), 0)
  }

}

object DlGamerScraper {

  val name = "DlGamer"

  val storeHead =
    "http://www.dlgamer.us/download-pc_games-c-27.html?page="

  def getFinalPage(storeTail: String = "1"): Int = {
    val doc = Jsoup.connect(storeHead + storeTail).get()
    val paginator = doc.getElementsByClass("numberpage").select("a").toList
    val last = paginator.last.text
    if (last == storeTail) storeTail.toInt
    else getFinalPage(last)
  }

  // lol solution
  val finalPage = 40

  def reindex = {
    for (i <- (1 to finalPage).par) {
      val D = DlGamerScraper(i).getAll
      D flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertGame(x))
      D flatMap (x => catching(classOf[PSQLException]) opt GwithP.insertPrice(x))
    }
  }

}

