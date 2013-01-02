package scrapers

import java.lang.ExceptionInInitializerError
import java.net.SocketTimeoutException
import java.util.Date

import scala.Option.option2Iterable
import scala.collection.JavaConversions.asScalaBuffer
import scala.util.control.Exception.catching

import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.postgresql.util.PSQLException

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.actor.actorRef2Scala
import akka.routing.SmallestMailboxRouter
import akka.util.duration.longToDurationLong
import anorm.NotAssigned
import models.GSwP
import models.Game
import models.GwithP
import models.Price
import models.SteamGame

import scala.util.control.Exception._


class SteamPkScraper extends Scraper {

  def receive = {
    case FetchGame(pageN) => sender ! SteamPkScraper.getAllSteam(pageN)
  }

}

object SteamPkScraper {

  private def appId(url: String): Int = {
    url.substring(34, url.indexOf('/', 34)).toInt
  }

  private def scorefS(meta: String): Int = {
    if (meta.isEmpty) 0
    else meta.toInt
  }

  private val name = "Steam"

  private val storeHead =
  "http://store.steampowered.com/search/results?&cc=us&category1=996&page="

  val finalPage = {
    val url = storeHead + "1"
    val ping = allCatch opt Jsoup.connect(url)
    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
    .timeout(3000).execute()
    val doc = Scraper.checkSite(url, ping).getOrElse(org.jsoup.nodes.Document.createShell(""))

    if (!doc.body.hasText) 1
    else {
      val navNumStr = doc.getElementsByClass("search_pagination_right").head.select("a").text
      val navNumSep = navNumStr.split(' ')

      if (navNumSep.length > 2) navNumSep(2).toInt
      else 1
    }
  }

  private def getAllSteam(pageN: Int): GameFetchedS = {

    val url = SteamPkScraper.storeHead + pageN.toString
    val ping = allCatch opt Jsoup.connect(url)
    .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/535.21 (KHTML, like Gecko) Chrome/19.0.1042.0 Safari/535.21")
    .timeout(3000).execute()

    def steamAllVals(html: Element): GSwP = GSwP(steamVals(html), allVals(html))
    def allVals(html: Element): GwithP = GwithP(gameVals(html), priceVals(html))

    def gameVals(html: Element): Game = {
      val name = html.select("h4").text
      val gameUrl = html.select("a").attr("href")
      val imgUrl = html.getElementsByClass("search_capsule").select("img").attr("src")

      Game(NotAssigned, name, SteamPkScraper.name, gameUrl, imgUrl)
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

    val doc = Scraper.checkSite(url, ping).getOrElse(org.jsoup.nodes.Document.createShell(""))

    if (!doc.body.hasText) GameFetchedS(List(), pageN, false)
    else {
      val searchResults = doc.getElementsByClass("search_result_row").toList
      GameFetchedS(searchResults map (steamAllVals), pageN, true)
    }
  }

}

class SteamPkMaster(listener: ActorRef) extends Actor {

  private val StPkfetcher = context.actorOf(Props[SteamPkScraper].withRouter(SmallestMailboxRouter(8)), name = "StPkfetcher")

  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  val finalPage = SteamPkScraper.finalPage

  println(finalPage)

  def receive = {
    case Scrape => for (i <- 1 to finalPage) StPkfetcher ! FetchGame(i)
    case GameFetchedS(gl, _, true) => {
      gl flatMap (x => catching(classOf[PSQLException]) opt GSwP.insertGame(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GSwP.insertPrice(x))
      gl flatMap (x => catching(classOf[PSQLException]) opt GSwP.insertSteam(x))

      printf("St:%d ".format(nrOfResults))
      nrOfResults += 1

      if (nrOfResults == finalPage) {
        println("St done in: %s ".format((System.currentTimeMillis - start).millis))

        listener ! Finished("Steam", (System.currentTimeMillis - start).millis)
        context.stop(self)
      }
    }
    case GameFetchedS(List(), pageN, false) => {
      println("Problem on St page: " + pageN.toString)
      nrOfResults += 1

      if (nrOfResults == finalPage) {
        printf("St done in: %s ".format((System.currentTimeMillis - start).millis))

        listener ! Finished("Steam", (System.currentTimeMillis - start).millis)
        context.stop(self)
      }
    }
    case e => {
      println("Printing the error:")
      println(e)
    }
  }

}