package models

import anorm.NotAssigned
import java.util.Date

import org.jsoup._
import org.jsoup.nodes._
import scala.collection.JavaConversions._

import scala.util.control.Exception._
import org.postgresql.util._

import akka.actor.Actor
import akka.actor.Props
import akka.util.duration._
import akka.util.Duration

class SteamScraper extends Scraper {

  def receive = {
    case FetchGame(pageN) => { sender ! SteamScraper.getAllSteam(pageN)
    println("received") }
  }

}

object SteamScraper {

  private def appId(url: String): Int = {
    url.substring(34, url.indexOf('/', 34)).toInt
  }

  private def scorefS(meta: String): Int = {
    if (meta.isEmpty) 0
    else meta.toInt
  }

  private val name = "Steam"

  private val storeHead =
    "http://store.steampowered.com/search/results?&cc=us&category1=998&page="

  val finalPage = {
    val doc = Jsoup.connect(storeHead + "1").get()
    val navNumStr = doc.getElementsByClass("search_pagination_right").select("a").text
    val navNumSep = navNumStr.split(' ')

    navNumSep(2).toInt
  }

  private def getAllSteam(pageN: Int): GameFetchedS = {

    val doc = Jsoup.connect(SteamScraper.storeHead + pageN.toString)
      .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
      .timeout(30000)
      .get()

    val searchResults = doc.getElementsByClass("search_result_row").toList

    def steamAllVals(html: Element): GSwP = GSwP(steamVals(html), allVals(html))
    def allVals(html: Element): GwithP = GwithP(gameVals(html), priceVals(html))

    def gameVals(html: Element): Game = {
      val name = html.select("h4").text
      val gameUrl = html.select("a").attr("href")
      val imgUrl = html.getElementsByClass("search_capsule").select("img").attr("src")

      Game(NotAssigned, name, SteamScraper.name, gameUrl, imgUrl)
    }

    def priceVals(html: Element): Price = {
      val name = html.select("h4").text
      val priceS = Scraper.$anitizer(html.getElementsByClass("search_price").text)
      val onSale = !html.select("strike").isEmpty

      Price(NotAssigned, priceS, onSale, new Date(), 0)
    }

    def steamVals(html: Element): SteamGame = {
      val name = html.select("h4").text
      val releaseDate = html.getElementsByClass("search_released").text
      val meta = scorefS(html.getElementsByClass("search_metascore").text)
      val gameUrl = html.select("a").attr("href")
      val gameId = appId(gameUrl)

      SteamGame(gameId, name, releaseDate, meta, 0)
    }

    GameFetchedS(searchResults map (steamAllVals), pageN)
  }

}

class SteamMaster extends Actor {

  val fetcher = context.actorOf(Props[SteamScraper])

  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  println(SteamScraper.finalPage)

  def receive = {
    case Scrape => for (i <- 1 to SteamScraper.finalPage) fetcher ! FetchGame(i)
    case GameFetchedS(gl, pageN) => {
      gl flatMap (x => catching(classOf[PSQLException]) opt GSwP.insertGame(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GSwP.insertPrice(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GSwP.insertSteam(x))

      printf("S:%d ".format(nrOfResults))
      nrOfResults += 1

      if (nrOfResults == SteamScraper.finalPage) {
        println("All done in: %s\n".format((System.currentTimeMillis - start).millis))
        context.stop(self)
      }
    }
    case e => {
      println("Printing the error:")
      println(e)
    }
  }

}