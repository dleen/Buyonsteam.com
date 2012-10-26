package models

import anorm._

import java.util.Date
import java.text.SimpleDateFormat

// jsoup html parser
import org.jsoup._
import org.jsoup.nodes._

/* Allows us to treat Java objects as Scala objects.
   The main use is that jsoup.select.Elements is an instantiation
   of java.util.List[Element]. So this collection allows us
   to use the Scala map function on a Java list. */
import scala.collection.JavaConversions._

abstract class Scraper {

  def scrapePage[A](f: (Element) => A): List[A]

  // Capture everything from html, useful for initializing, creating new games.
  def allVals(html: Element): Combined = Combined(gameVals(html), priceVals(html))

  // Capture the game values
  def gameVals(html: Element): Game

  // Capture the price values
  def priceVals(html: Element): Price

}

abstract class StoreDetails {
  val storeHead: String
  val finalPage: Int
}

class SteamScraper(pageN: Int) extends Scraper with SafeMoney {

  def scrapePage[A](f: (Element) => A): List[A] = {
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

    Price(NotAssigned, name, priceS, None, new Date(), None)
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
    "http://store.steampowered.com/search/results?&category1=998&page="

  val finalPage = {
    val doc = Jsoup.connect(storeHead + "1").get()
    val navNumStr = doc.getElementsByClass("search_pagination_right").select("a").text
    val navNumSep = navNumStr.split(' ')

    navNumSep(2).toInt
  }
}

trait SafeMoney {

  def rm$(s: String) = s.tail.toDouble
  def $anitizer(price: String): Option[Double] = {
    // "$59.99" -> 59.99 

    if (price.isEmpty || price.contains("Free") || price.contains("Demo")) None
    // "$59.99 $49.99" -> 49.99
    else if (price.contains(' ')) {
      val discount = price.split(' ')
      Some(math.min(rm$(discount(0)), rm$(discount(1))))
    } else if (price(0) == '$') Some(rm$(price))
    else None
  }
}