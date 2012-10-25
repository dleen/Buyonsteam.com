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

object steamScraper {

  val steamStoreHead =
    "http://store.steampowered.com/search/?snr=1_4_4__12&term=#sort_by=&sort_order=ASC&page="

  def finalPage = {
    val doc = Jsoup.connect(steamStoreHead + "1").get()
    val listedPageNumbers = doc.getElementsByClass("search_pagination_right").select("a").text
    val pageNums = listedPageNumbers.split(' ')
    pageNums(2).toInt
  }

  def getSteamAppId(url: String): Option[Int] = {
    if (url.length < 42) None
    else if (url(30) == 'v') None
    else Some(url.substring(34, url.indexOf('/', 34)).toInt)
  }

  def gameValues(gameHtml: org.jsoup.nodes.Element): Game = {

    val appUrl = gameHtml.select("a").attr("href")
    val appId = getSteamAppId(appUrl)

    def scoreSanit(mScore: String): Option[Int] = {
      if (mScore.isEmpty) None
      else Some(mScore.toInt)
    }

    Game(NotAssigned,
      appId,
      gameHtml.select("h4").text,
      appUrl,
      gameHtml.getElementsByClass("search_capsule").select("img").attr("src"),
      gameHtml.getElementsByClass("search_released").text,
      scoreSanit(gameHtml.getElementsByClass("search_metascore").text))
  }

  def scrapeGamePage(pageN: Int) = {
    val doc = Jsoup.connect(steamStoreHead + pageN.toString).get()
    val searchResults = doc.getElementsByClass("search_result_row")

    searchResults.map(gameValues(_))
  }

  //def gamePrice(): PriceHistory = {
  //  gameHtml.getElementsByClass("search_price").text
  //}

}