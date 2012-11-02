package models

import play.api._
import play.api.mvc._

import akka.util.duration._
import akka.util.Duration

import play.api.libs.concurrent._

import play.api.Play.current

import play.libs.Akka

import anorm._
import java.util.Date
import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._

case class SteamScraper(pageN: Int = 1) extends Scraper with SafeMoney {

  def getAll: List[GwithP] = scrapePage(pageN, allVals)
  def getGames: List[Game] = scrapePage(pageN, gameVals)
  def getPrices: List[Price] = scrapePage(pageN, priceVals)

  def getAllSteam: List[GSwP] = scrapePage[GSwP](pageN, steamAllVals)
  def steamAllVals(html: Element): GSwP =
    GSwP(steamVals(html), GwithP(gameVals(html), priceVals(html)))

  def scrapePage[A](pageN: Int, f: (Element) => A): List[A] = {
    val doc = Jsoup.connect(SteamScraper.storeHead + pageN.toString)
      .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
      .get()
    val searchResults = doc.getElementsByClass("search_result_row").toList

    searchResults.map(f(_))
  }

  def gameVals(html: Element): Game = {
    val name = html.select("h4").text
    val gameUrl = html.select("a").attr("href")
    val imgUrl = html.getElementsByClass("search_capsule").select("img").attr("src")

    Game(NotAssigned, name, SteamScraper.name, gameUrl, imgUrl)
  }

  def priceVals(html: Element): Price = {
    val name = html.select("h4").text
    val priceS = $anitizer(html.getElementsByClass("search_price").text)
    val onSale = !html.select("strike").isEmpty

    Price(NotAssigned, priceS, onSale, new Date(), 0)
  }

  def steamVals(html: Element): SteamGame = {
    val name = html.select("h4").text
    val releaseDate = html.getElementsByClass("search_released").text
    val meta = SteamScraper.scorefS(html.getElementsByClass("search_metascore").text)
    val gameUrl = html.select("a").attr("href")

    val gameId = SteamScraper.appId(gameUrl)

    SteamGame(gameId, name, releaseDate, meta, 0)
  }
}

object SteamScraper {

  def appId(url: String): Int = {
    url.substring(34, url.indexOf('/', 34)).toInt
  }

  def scorefS(meta: String): Int = {
    if (meta.isEmpty) 0
    else meta.toInt
  }

  val name = "Steam"

  val storeHead =
    "http://store.steampowered.com/search/results?&cc=us&category1=998&page="

  val finalPage = {
    val doc = Jsoup.connect(storeHead + "1").get()
    val navNumStr = doc.getElementsByClass("search_pagination_right").select("a").text
    val navNumSep = navNumStr.split(' ')

    navNumSep(2).toInt
  }

  def reindex = {
    for (i <- (1 to finalPage).par) {
      SteamScraper(i).getAllSteam map { x =>
        try {
          GSwP.insertGame(x)
          println("Inserting game")
        } catch { case e => println(e) }
        try {
          GSwP.insertPrice(x)
          println("Inserting PRICE")
        } catch { case e => println(e) }
        try {
          GSwP.insertSteam(x)
          println("Inserting steam values")
        } catch { case e => println(e) }
      }
    }
  }

}