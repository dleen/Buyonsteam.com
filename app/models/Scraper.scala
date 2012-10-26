package models

import anorm._

import java.util.Date
import java.text.SimpleDateFormat

// jsoup html parser
import org.jsoup._

/* Allows us to treat Java objects as Scala objects.
   The main use is that jsoup.select.Elements is an instantiation
   of java.util.List[Element]. So this collection allows us
   to use the Scala map function on a Java list. */
import scala.collection.JavaConversions._

// CHANGE TO CLASS
// OBJECT IS STATIC!!!
object steamScraper {

  val steamStoreHead =
    "http://store.steampowered.com/search/?snr=1_4_4__12&term=#sort_by=&sort_order=ASC&category1=998&page="

  def finalPage = {
    val doc = Jsoup.connect(steamStoreHead + "1").get()
    val listedPageNumbers = doc.getElementsByClass("search_pagination_right").select("a").text
    val pageNums = listedPageNumbers.split(' ')

    pageNums(2).toInt
  }

  def steamGameValues(gameHtml: org.jsoup.nodes.Element): Combined = {

    def getSteamAppId(url: String): Option[Int] = {
      if (url.length < 42) None
      else if (url(30) == 'v') None
      else Some(url.substring(34, url.indexOf('/', 34)).toInt)
    }

    def scoreSanitizer(meta: String): Option[Int] = {
      if (meta.isEmpty) None
      else Some(meta.toInt)
    }

    def dollarSanitizer(dollarPrice: String): Option[Double] = {
      def removeDollar(s: String) = s.tail.toDouble

      if (dollarPrice.isEmpty || dollarPrice.contains("Free")
        || dollarPrice.contains("Demo")) None
      else if (dollarPrice.contains(' ')) {
        val discount = dollarPrice.split(' ')
        Some(math.min(removeDollar(discount(0)), removeDollar(discount(1))))
      } else if (dollarPrice(0) == '$') Some(removeDollar(dollarPrice))
      else None
    }

    val gameUrl = gameHtml.select("a").attr("href")
    val gameId = getSteamAppId(gameUrl)
    val gameName = gameHtml.select("h4").text
    val imgUrl = gameHtml.getElementsByClass("search_capsule").select("img").attr("src")
    val releaseDate = gameHtml.getElementsByClass("search_released").text
    val meta = scoreSanitizer(gameHtml.getElementsByClass("search_metascore").text)
    val pOS = dollarSanitizer(gameHtml.getElementsByClass("search_price").text)

    Combined(
      Game(NotAssigned, gameId, gameName, gameUrl, imgUrl, releaseDate, meta),
      Price(NotAssigned, gameName, pOS, None, new Date(), None))
  }

  def scrapeGamePage(pageN: Int): List[Combined] = {
    val doc = Jsoup.connect(steamStoreHead + pageN.toString).get()
    val searchResults = doc.getElementsByClass("search_result_row").toList

    searchResults.map(steamGameValues(_)).filter(x => x.p.priceOnSteam != None)
  }

}